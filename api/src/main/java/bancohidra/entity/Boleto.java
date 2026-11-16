package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import bancohidra.util.CodigoBoleto;
import bancohidra.util.Dinheiro;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Boleto de cobrança emitido por um cliente (o beneficiário) e pago por outro cliente.
 *
 * <p>Como o saldo da {@link Conta}, o status é <b>encapsulado</b>: só muda por {@link #registrarPagamento}
 * e {@link #cancelar}, que conferem se o boleto ainda está aberto. Depois do vencimento, o boleto
 * continua pagável, com multa e juros calculados por {@link #encargosEm}.
 */
@Entity
public class Boleto extends PanacheEntity {

    /** Multa de 2% (o máximo permitido pelo Código de Defesa do Consumidor). */
    public static final BigDecimal MULTA = new BigDecimal("0.02");
    /** Juros de mora de 1% ao mês, cobrados por dia de atraso (1/30 por dia). */
    public static final BigDecimal JUROS_AO_MES = new BigDecimal("0.01");
    public static final int PRAZO_MAXIMO_DIAS = 365;

    @Column(nullable = false, unique = true, length = 44)
    public String codigoBarras;

    @Column(nullable = false, unique = true, length = 47)
    public String linhaDigitavel;

    @ManyToOne(optional = false)
    public Conta beneficiario;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal valor;

    @Column(nullable = false)
    public LocalDate vencimento;

    @Column(length = 100)
    public String descricao;

    @Column(nullable = false)
    public LocalDateTime emitidoEm = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusBoleto status = StatusBoleto.ABERTO;

    @ManyToOne
    private Conta pagador;

    private LocalDateTime pagoEm;

    @Column(precision = 14, scale = 2)
    private BigDecimal valorPago;

    /** Mesma autenticação das transações do pagamento (liga o boleto ao comprovante). */
    private UUID autenticacao;

    /** Multa e juros de um pagamento numa certa data (zero até o vencimento). */
    public record Encargos(BigDecimal valor, long diasAtraso, BigDecimal multa, BigDecimal juros) {
        public BigDecimal total() {
            return valor.add(multa).add(juros);
        }
    }

    // ---------- emissão ----------

    public static Boleto emitir(Conta beneficiario, BigDecimal valor, LocalDate vencimento, String descricao,
                                LocalDate hoje) {
        if (!beneficiario.isAtiva()) {
            throw new RegraNegocioException("Sua conta não está ativa para emitir boletos");
        }
        if (vencimento == null || vencimento.isBefore(hoje)) {
            throw new RegraNegocioException("O vencimento deve ser hoje ou uma data futura");
        }
        if (vencimento.isAfter(hoje.plusDays(PRAZO_MAXIMO_DIAS))) {
            throw new RegraNegocioException("O vencimento pode ser de no máximo " + PRAZO_MAXIMO_DIAS + " dias");
        }
        Boleto b = new Boleto();
        b.beneficiario = beneficiario;
        b.valor = Dinheiro.exigirPositivo(valor);
        b.vencimento = vencimento;
        b.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
        do {
            b.codigoBarras = CodigoBoleto.codigoBarras(vencimento, b.valor, campoLivre(beneficiario));
        } while (count("codigoBarras", b.codigoBarras) > 0);
        b.linhaDigitavel = CodigoBoleto.linhaDigitavel(b.codigoBarras);
        return b;
    }

    /** Campo livre (25 dígitos): agência (4) + "nosso número" sorteado (14) + conta sem o traço (7). */
    private static String campoLivre(Conta conta) {
        long nossoNumero = ThreadLocalRandom.current().nextLong(1, 100_000_000_000_000L);
        return conta.agencia + String.format("%014d", nossoNumero) + conta.numero.replace("-", "");
    }

    // ---------- regras ----------

    public Encargos encargosEm(LocalDate data) {
        long dias = Math.max(0, ChronoUnit.DAYS.between(vencimento, data));
        if (dias == 0) {
            return new Encargos(valor, 0, zero(), zero());
        }
        BigDecimal multa = valor.multiply(MULTA).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal juros = valor.multiply(JUROS_AO_MES).multiply(BigDecimal.valueOf(dias))
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_EVEN);
        return new Encargos(valor, dias, multa, juros);
    }

    public boolean vencidoEm(LocalDate data) {
        return status == StatusBoleto.ABERTO && data.isAfter(vencimento);
    }

    public void exigirAberto() {
        switch (status) {
            case PAGO -> throw new RegraNegocioException("Este boleto já foi pago");
            case CANCELADO -> throw new RegraNegocioException("Este boleto foi cancelado por quem emitiu");
            default -> {
            }
        }
    }

    public void registrarPagamento(Conta quemPagou, BigDecimal total, UUID autenticacaoPagamento,
                                   LocalDateTime agora) {
        exigirAberto();
        status = StatusBoleto.PAGO;
        pagador = quemPagou;
        valorPago = total;
        autenticacao = autenticacaoPagamento;
        pagoEm = agora;
    }

    public void cancelar() {
        exigirAberto();
        status = StatusBoleto.CANCELADO;
    }

    public StatusBoleto getStatus() {
        return status;
    }

    public Conta getPagador() {
        return pagador;
    }

    public LocalDateTime getPagoEm() {
        return pagoEm;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public UUID getAutenticacao() {
        return autenticacao;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }

    // ---------- consultas ----------

    public static List<Boleto> emitidosPor(Conta conta) {
        return list("beneficiario", Sort.descending("emitidoEm", "id"), conta);
    }
}
