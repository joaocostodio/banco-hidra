package bancohidra.dto;

import bancohidra.dto.PixDtos.DestinatarioResponse;
import bancohidra.entity.Boleto;
import bancohidra.entity.StatusBoleto;
import bancohidra.util.CodigoBoleto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class BoletoDtos {

    private BoletoDtos() {
    }

    public record EmitirBoletoRequest(
            @NotNull(message = "Informe o valor") BigDecimal valor,
            @NotNull(message = "Informe o vencimento") LocalDate vencimento,
            @Size(max = 100, message = "Descrição com no máximo 100 caracteres") String descricao) {
    }

    public record PagarBoletoRequest(@NotBlank(message = "Informe a linha digitável") String linha) {
    }

    /** Boleto emitido por mim (lista e tela de impressão). */
    public record BoletoResponse(Long id, String linhaDigitavel, String codigoBarras, BigDecimal valor,
                                 LocalDate vencimento, String descricao, StatusBoleto status, boolean vencido,
                                 LocalDateTime emitidoEm, String beneficiarioNome, String agencia, String conta,
                                 String pagadorNome, LocalDateTime pagoEm, BigDecimal valorPago,
                                 UUID autenticacao) {
        public static BoletoResponse de(Boleto b, LocalDate hoje) {
            return new BoletoResponse(b.id, CodigoBoleto.formatar(b.linhaDigitavel), b.codigoBarras, b.valor,
                    b.vencimento, b.descricao, b.getStatus(), b.vencidoEm(hoje), b.emitidoEm,
                    b.beneficiario.titular.nome, b.beneficiario.agencia, b.beneficiario.numero,
                    b.getPagador() == null ? null : b.getPagador().titular.nome,
                    b.getPagoEm(), b.getValorPago(), b.getAutenticacao());
        }
    }

    /** O que o pagador confere antes de pagar: para quem vai e quanto vai pagar hoje. */
    public record ConsultaBoletoResponse(String linhaDigitavel, DestinatarioResponse beneficiario,
                                         String descricao, LocalDate vencimento, BigDecimal valor,
                                         long diasAtraso, BigDecimal multa, BigDecimal juros, BigDecimal total,
                                         StatusBoleto status) {
    }
}
