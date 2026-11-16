package bancohidra.dto;

import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Transacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class TransacaoDtos {

    private TransacaoDtos() {
    }

    /**
     * Uma linha do extrato. {@code interna}: transferência entre duas contas do mesmo cliente; só nesse caso
     * vem o nome da outra conta ({@code contraparteContaNome}), porque o nome que alguém dá às próprias contas
     * ("Reserva para cirurgia") não deve aparecer para outras pessoas.
     */
    public record TransacaoResponse(Long id, Long contaId, String contaNumero, String contaNome, TipoTransacao tipo,
                                    BigDecimal valor, BigDecimal saldoApos, String descricao, Long categoriaId,
                                    String categoria, String contraparteNome, String contraparteConta,
                                    boolean interna, String contraparteContaNome,
                                    UUID autenticacao, LocalDateTime dataHora) {
        public static TransacaoResponse de(Transacao t) {
            boolean interna = t.contraparte != null && t.contraparte.titular.id.equals(t.conta.titular.id);
            return new TransacaoResponse(t.id, t.conta.id, t.conta.numero, t.conta.nome, t.tipo, t.valor,
                    t.saldoApos, t.descricao, t.categoria.id, t.categoria.descricao,
                    t.contraparte == null ? null : t.contraparte.titular.nome,
                    t.contraparte == null ? null : t.contraparte.numero,
                    interna, interna ? t.contraparte.nome : null,
                    t.autenticacao, t.dataHora);
        }
    }

    /**
     * Transferência feita pelo cliente. A origem é uma conta dele ({@code contaOrigemId}; sem ela, a principal);
     * o destino, agência + número (de outra pessoa ou de outra conta dele mesmo).
     */
    public record TransferenciaRequest(
            Long contaOrigemId,
            @NotBlank(message = "Informe a agência") @Size(max = 4) String agencia,
            @NotBlank(message = "Informe o número da conta") @Size(max = 8) String numero,
            @NotNull(message = "Informe o valor") BigDecimal valor,
            @Size(max = 200) String descricao) {
    }

    /** Comprovante: dados de quem pagou e de quem recebeu, a partir da autenticação. */
    public record ComprovanteResponse(UUID autenticacao, String categoria, BigDecimal valor, String descricao,
                                      LocalDateTime dataHora, Parte origem, Parte destino) {

        public record Parte(String nome, String agencia, String conta) {
        }

        public static ComprovanteResponse de(List<Transacao> pernas) {
            Transacao saida = pernas.stream().filter(t -> t.tipo == TipoTransacao.SAIDA).findFirst().orElse(null);
            Transacao entrada = pernas.stream().filter(t -> t.tipo == TipoTransacao.ENTRADA).findFirst().orElse(null);
            Transacao base = saida != null ? saida : entrada;
            return new ComprovanteResponse(base.autenticacao, base.categoria.descricao, base.valor, base.descricao,
                    base.dataHora,
                    saida == null ? null : parte(saida),
                    entrada == null ? null : parte(entrada));
        }

        private static Parte parte(Transacao t) {
            return new Parte(t.conta.titular.nome, t.conta.agencia, t.conta.numero);
        }
    }
}
