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

    /**
     * O que aparece para quem vai pagar ("você está pagando..."), sem expor dados completos.
     * {@code tipoChave} vem preenchido só quando a busca foi por chave Pix.
     */
    public record DestinatarioResponse(String nome, String cpfMascarado, String banco, String agencia,
                                       String conta, TipoChavePix tipoChave) {
    }

    /** Cobrança por QR code: uma das minhas chaves; sem valor, quem paga escolhe quanto. */
    public record CobrancaRequest(
            @NotNull(message = "Escolha a chave que vai receber") Long chaveId,
            BigDecimal valor,
            @Size(max = 60, message = "Mensagem com no máximo 60 caracteres") String descricao) {
    }

    /** {@code codigo} é o "Pix copia e cola"; o site desenha o QR code a partir dele. */
    public record CobrancaResponse(String codigo, String chave, TipoChavePix tipoChave, BigDecimal valor,
                                   String descricao) {
    }

    public record LerCodigoRequest(@NotBlank(message = "Cole o código Pix") String codigo) {
    }

    /** O que o pagador vê depois de colar o código, antes de confirmar. Valor nulo: ele digita. */
    public record CodigoLidoResponse(String chave, BigDecimal valor, String descricao,
                                     DestinatarioResponse destinatario) {
    }

    /** A origem é sempre a conta de quem está logado. */
    public record PixRequest(
            @NotBlank(message = "Informe a chave Pix") String chave,
            @NotNull(message = "Informe o valor") BigDecimal valor,
            @Size(max = 200) String descricao) {
    }
}
