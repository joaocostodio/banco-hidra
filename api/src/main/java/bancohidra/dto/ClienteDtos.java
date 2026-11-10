package bancohidra.dto;

import bancohidra.entity.Conta;
import bancohidra.entity.Perfil;
import bancohidra.entity.Usuario;
import bancohidra.util.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    /** Cadastro de cliente: o banco abre a conta junto. */
    public record NovoClienteRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o CPF") String cpf,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 150) String email) {
    }

    /** Na edição o CPF não muda (é a identidade do cliente). */
    public record EditarClienteRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 150) String email) {
    }

    /** Resposta do cadastro: a senha provisória aparece só esta vez, para o ADM entregar ao cliente. */
    public record NovoClienteResponse(ClienteResponse cliente, String senhaProvisoria) {
    }

    public record SenhaProvisoriaResponse(String senhaProvisoria) {
    }

    /** {@code primeiroAcesso = true}: o cliente ainda não trocou a senha provisória. */
    public record ClienteResponse(Long id, String nome, String cpf, String email, Perfil perfil,
                                  LocalDateTime criadoEm, boolean primeiroAcesso, ContaDtos.ContaResumo conta) {
        public static ClienteResponse de(Usuario u, Conta conta) {
            return new ClienteResponse(u.id, u.nome, u.cpf == null ? null : Cpf.formatar(u.cpf), u.email, u.perfil,
                    u.criadoEm, u.isTrocarSenha(), conta == null ? null : ContaDtos.ContaResumo.de(conta));
        }
    }
}
