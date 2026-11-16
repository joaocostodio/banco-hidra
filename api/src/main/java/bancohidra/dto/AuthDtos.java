package bancohidra.dto;

import bancohidra.entity.Perfil;
import bancohidra.entity.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** {@code email}: o e-mail do cliente ou o login do administrador (ex.: "joao"). */
    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail ou usuário") @Size(max = 150) String email,
            @NotBlank(message = "Informe a senha") @Size(max = 72) String senha) {
    }

    /** Cadastro feito pela própria pessoa, na tela "Criar conta". A senha segue a regra de senha forte. */
    public record CadastroRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o CPF") @Size(max = 20) String cpf,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 150) String email,
            @NotBlank(message = "Informe a senha") @Size(max = 72) String senha) {
    }

    public record TrocarSenhaRequest(
            @NotBlank(message = "Informe a senha atual") @Size(max = 72) String senhaAtual,
            @NotBlank(message = "Informe a nova senha") @Size(max = 72) String novaSenha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.id, u.nome, u.email, u.perfil);
        }
    }

    /**
     * Resposta do login. O token <b>não</b> vem aqui: vai num cookie httpOnly, que o JavaScript não lê.
     * {@code expiraEm} serve para o site saber quando a sessão acaba. {@code trocarSenha = true}: a sessão
     * só serve para trocar a senha provisória.
     */
    public record LoginResponse(Instant expiraEm, boolean trocarSenha, UsuarioResponse usuario) {
    }
}
