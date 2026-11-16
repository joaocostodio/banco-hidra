package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

/**
 * Pessoa que acessa o banco: um cliente (dono de conta) ou um administrador.
 *
 * <p>A senha também é encapsulada: só o hash BCrypt é guardado, ele nunca sai da entidade,
 * e a própria entidade controla as tentativas erradas (bloqueio temporário após várias falhas).
 */
@Entity
public class Usuario extends PanacheEntity {

    public static final int TENTATIVAS_MAXIMAS = 5;
    public static final int MINUTOS_BLOQUEIO = 15;
    public static final int TAMANHO_MINIMO_SENHA = 8;

    @Column(nullable = false, length = 150)
    public String nome;

    /** Somente os 11 dígitos. Obrigatório para clientes; administradores não têm. */
    @Column(unique = true, length = 11)
    public String cpf;

    @Column(nullable = false, unique = true, length = 150)
    public String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Perfil perfil = Perfil.CLIENTE;

    @Column(nullable = false)
    public LocalDateTime criadoEm = LocalDateTime.now();

    @JsonIgnore
    @Column(length = 60)
    private String senhaHash;

    /** Verdadeiro enquanto o usuário estiver com a senha provisória dada pelo banco. */
    @Column(nullable = false)
    private boolean trocarSenha;

    @Column(nullable = false)
    private int tentativasFalhas;

    private LocalDateTime bloqueadoAte;

    // ---------- senha ----------

    /** Senha escolhida pelo próprio usuário. */
    public void definirSenha(String novaSenha) {
        exigirSenhaForte(novaSenha);
        senhaHash = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
        trocarSenha = false;
        desbloquearAcesso();
    }

    /**
     * Senha do administrador inicial, vinda da configuração do servidor. Não passa pela regra de
     * senha forte: quem escolhe é o dono do servidor (o painel avisa enquanto ela for a padrão).
     */
    public void definirSenhaDoServidor(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException("Configure a senha do administrador (banco.admin.senha)");
        }
        senhaHash = BCrypt.hashpw(senha, BCrypt.gensalt());
        trocarSenha = false;
        desbloquearAcesso();
    }

    /** Senha gerada pelo banco: funciona para entrar, mas obriga a troca no primeiro acesso. */
    public void definirSenhaProvisoria(String senhaProvisoria) {
        senhaHash = BCrypt.hashpw(senhaProvisoria, BCrypt.gensalt());
        trocarSenha = true;
        desbloquearAcesso();
    }

    /**
     * Confere a senha do login, contando as falhas. Na 5ª falha seguida o acesso
     * fica bloqueado por 15 minutos (proteção contra quem tenta adivinhar senhas).
     */
    public boolean autenticar(String senha, LocalDateTime agora) {
        if (acessoBloqueado(agora)) {
            return false;
        }
        if (senhaHash != null && senha != null && BCrypt.checkpw(senha, senhaHash)) {
            tentativasFalhas = 0;
            bloqueadoAte = null;
            return true;
        }
        tentativasFalhas++;
        if (tentativasFalhas >= TENTATIVAS_MAXIMAS) {
            bloqueadoAte = agora.plusMinutes(MINUTOS_BLOQUEIO);
            tentativasFalhas = 0;
        }
        return false;
    }

    public boolean confereSenha(String senha) {
        return senhaHash != null && senha != null && BCrypt.checkpw(senha, senhaHash);
    }

    public boolean acessoBloqueado(LocalDateTime agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    public LocalDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public boolean isTrocarSenha() {
        return trocarSenha;
    }

    private void desbloquearAcesso() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    private static void exigirSenhaForte(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres");
        }
        if (senha.length() > 72) {
            throw new RegraNegocioException("A senha deve ter no máximo 72 caracteres");
        }
        if (!senha.matches(".*[A-Za-z].*") || !senha.matches(".*\\d.*")) {
            throw new RegraNegocioException("A senha deve ter letras e números");
        }
    }

    // ---------- consultas ----------

    public static boolean existeCpf(String cpf) {
        return count("cpf", cpf) > 0;
    }

    public static boolean existeEmail(String email, Long ignorarId) {
        return count("lower(email) = lower(?1) and id <> ?2", email, ignorarId == null ? -1L : ignorarId) > 0;
    }

    public static Usuario porEmail(String email) {
        return find("lower(email) = lower(?1)", email.trim()).firstResult();
    }
}
