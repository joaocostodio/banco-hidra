package bancohidra.dto;

import bancohidra.entity.Perfil;
import bancohidra.entity.Usuario;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** {@code email}: o e-mail do cliente ou o login do administrador (ex.: "joao"). */
    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail ou usuário") String email,
            @NotBlank(message = "Informe a senha") String senha) {
    }

    public record TrocarSenhaRequest(
            @NotBlank(message = "Informe a senha atual") String senhaAtual,
            @NotBlank(message = "Informe a nova senha") String novaSenha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.id, u.nome, u.email, u.perfil);
        }
    }

    /** {@code trocarSenha = true}: o token só serve para trocar a senha. */
    public record LoginResponse(String token, Instant expiraEm, boolean trocarSenha, UsuarioResponse usuario) {
    }
}
