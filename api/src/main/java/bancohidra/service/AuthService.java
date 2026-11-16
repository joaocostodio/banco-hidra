package bancohidra.service;

import bancohidra.dto.AuthDtos.LoginResponse;
import bancohidra.dto.AuthDtos.UsuarioResponse;
import bancohidra.entity.Usuario;
import bancohidra.exception.RegraNegocioException;
import bancohidra.security.Papeis;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    /** Hash qualquer, usado para gastar o mesmo tempo quando o e-mail não existe. */
    private static final String HASH_FALSO = BCrypt.hashpw("nao-existe", BCrypt.gensalt());
    private static final String CREDENCIAIS_INVALIDAS = "Usuário ou senha incorretos";

    @ConfigProperty(name = "smallrye.jwt.new-token.lifespan", defaultValue = "3600")
    long validadeSegundos;

    /** Resultado do login. Não lançamos exceção aqui dentro para não desfazer a contagem de falhas. */
    public record Resultado(LoginResponse resposta, String erro) {
    }

    @Transactional
    public Resultado verificarCredenciais(String email, String senha) {
        Usuario usuario = Usuario.porEmail(email);
        LocalDateTime agora = LocalDateTime.now();
        if (usuario == null) {
            BCrypt.checkpw(senha, HASH_FALSO); // mesmo tempo de resposta: não revela se o e-mail existe
            return new Resultado(null, CREDENCIAIS_INVALIDAS);
        }
        if (usuario.acessoBloqueado(agora)) {
            return new Resultado(null, mensagemBloqueio(usuario));
        }
        if (!usuario.autenticar(senha, agora)) {
            return new Resultado(null, usuario.acessoBloqueado(agora) ? mensagemBloqueio(usuario) : CREDENCIAIS_INVALIDAS);
        }
        return new Resultado(emitir(usuario), null);
    }

    @Transactional
    public LoginResponse trocarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = Usuario.findById(usuarioId);
        if (usuario == null || !usuario.confereSenha(senhaAtual)) {
            throw new RegraNegocioException("Senha atual incorreta");
        }
        if (senhaAtual.equals(novaSenha)) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual");
        }
        usuario.definirSenha(novaSenha);
        return emitir(usuario); // token novo, agora com o papel verdadeiro
    }

    LoginResponse emitir(Usuario usuario) {
        Instant expira = Instant.now().plusSeconds(validadeSegundos);
        String papel = usuario.isTrocarSenha() ? Papeis.TROCAR_SENHA : usuario.perfil.name();
        String token = Jwt.subject(String.valueOf(usuario.id))
                .upn(usuario.email)
                .groups(Set.of(papel))
                .claim("nome", usuario.nome)
                .expiresAt(expira)
                .sign();
        return new LoginResponse(token, expira, usuario.isTrocarSenha(), UsuarioResponse.de(usuario));
    }

    private static String mensagemBloqueio(Usuario usuario) {
        return "Acesso bloqueado por excesso de tentativas. Tente novamente às "
                + usuario.getBloqueadoAte().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
