package bancohidra.dto;

import bancohidra.entity.Conta;
import bancohidra.entity.StatusConta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ContaDtos {

    private ContaDtos() {
    }

    public record ContaResponse(Long id, String agencia, String numero, Long titularId, String titularNome,
                                BigDecimal saldo, StatusConta status, LocalDateTime abertaEm) {
        public static ContaResponse de(Conta c) {
            return new ContaResponse(c.id, c.agencia, c.numero, c.titular.id, c.titular.nome,
                    c.getSaldo(), c.status, c.abertaEm);
        }
    }

    /** Versão curta, usada dentro de outras respostas. */
    public record ContaResumo(Long id, String agencia, String numero, StatusConta status, BigDecimal saldo) {
        public static ContaResumo de(Conta c) {
            return new ContaResumo(c.id, c.agencia, c.numero, c.status, c.getSaldo());
        }
    }

    public record DepositoRequest(
            @NotNull(message = "Informe o valor") BigDecimal valor,
            @Size(max = 200) String descricao) {
    }
}
