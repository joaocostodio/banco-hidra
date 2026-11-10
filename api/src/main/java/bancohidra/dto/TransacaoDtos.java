package bancohidra.dto;

import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Transacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class TransacaoDtos {

    private TransacaoDtos() {
    }

    public record TransacaoResponse(Long id, Long contaId, String contaNumero, TipoTransacao tipo,
                                    BigDecimal valor, BigDecimal saldoApos, String descricao, String categoria,
                                    String contraparteNome, String contraparteConta,
                                    UUID autenticacao, LocalDateTime dataHora) {
        public static TransacaoResponse de(Transacao t) {
            return new TransacaoResponse(t.id, t.conta.id, t.conta.numero, t.tipo, t.valor, t.saldoApos,
                    t.descricao, t.categoria.descricao,
                    t.contraparte == null ? null : t.contraparte.titular.nome,
                    t.contraparte == null ? null : t.contraparte.numero,
                    t.autenticacao, t.dataHora);
        }
    }

    public record TransferenciaRequest(
            @NotNull(message = "Informe a conta de origem") Long contaOrigemId,
            @NotNull(message = "Informe a conta de destino") Long contaDestinoId,
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
