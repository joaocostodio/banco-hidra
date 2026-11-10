package bancohidra.dto;

import bancohidra.entity.ChavePix;
import bancohidra.entity.TipoChavePix;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PixDtos {

    private PixDtos() {
    }

    /** Para chave ALEATORIA o valor é ignorado: o banco gera um UUID. */
    public record NovaChaveRequest(
            @NotNull(message = "Informe o tipo da chave") TipoChavePix tipo,
            @Size(max = 150) String valor) {
    }

    public record ChaveResponse(Long id, TipoChavePix tipo, String valor, LocalDateTime criadaEm) {
        public static ChaveResponse de(ChavePix c) {
            return new ChaveResponse(c.id, c.tipo, c.valor, c.criadaEm);
        }
    }

    /** O que aparece para quem vai pagar ("você está pagando..."), sem expor dados completos. */
    public record DestinatarioResponse(String nome, String cpfMascarado, String banco, String agencia,
                                       String conta, TipoChavePix tipoChave) {
    }

    public record PixRequest(
            @NotNull(message = "Informe a conta de origem") Long contaOrigemId,
            @NotBlank(message = "Informe a chave Pix") String chave,
            @NotNull(message = "Informe o valor") BigDecimal valor,
            @Size(max = 200) String descricao) {
    }
}
