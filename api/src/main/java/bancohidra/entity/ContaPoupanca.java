package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import bancohidra.util.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Poupança: rende {@value #TAXA_TEXTO} ao mês, pago no "aniversário" (o mesmo dia do mês em que foi aberta),
 * como a poupança de verdade. Simplificação: rende sobre o saldo do dia do aniversário (no banco real, só o
 * dinheiro que ficou o mês inteiro rende).
 *
 * <p>Pode ter uma <b>meta</b>: "guardar R$ 5.000 para a viagem até dezembro", com o progresso calculado pelo saldo.
 */
@Entity
@DiscriminatorValue("POUPANCA")
public class ContaPoupanca extends ContaRemunerada {

    public static final BigDecimal TAXA_MENSAL = new BigDecimal("0.005");
    static final String TAXA_TEXTO = "0,5%";

    @Column(name = "meta_valor", precision = 14, scale = 2)
    private BigDecimal metaValor;

    @Column(name = "meta_descricao", length = 60)
    private String metaDescricao;

    @Column(name = "meta_data")
    private LocalDate metaData;

    public ContaPoupanca() {
        nome = "Poupança";
    }

    @Override
    public TipoConta getTipo() {
        return TipoConta.POUPANCA;
    }

    /** Meses inteiros desde a última data-base; cada um rende 0,5% sobre o saldo (juros compostos). */
    @Override
    protected Rendimento calcularRendimento(LocalDate desde, LocalDate hoje) {
        long meses = ChronoUnit.MONTHS.between(desde, hoje);
        if (meses < 1) {
            return new Rendimento(BigDecimal.ZERO, desde);
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow((int) meses).subtract(BigDecimal.ONE);
        BigDecimal valor = getSaldo().multiply(fator).setScale(2, RoundingMode.HALF_EVEN);
        return new Rendimento(valor, desde.plusMonths(meses));
    }

    @Override
    public String descricaoRendimento() {
        return "Rendimento da poupança (" + TAXA_TEXTO + " ao mês)";
    }

    @Override
    public LocalDate proximoRendimento(LocalDate hoje) {
        LocalDate proximo = dataBase().plusMonths(1);
        while (proximo.isBefore(hoje)) {
            proximo = proximo.plusMonths(1);
        }
        return proximo;
    }

    // ---------- meta ----------

    public void definirMeta(BigDecimal valor, String descricao, LocalDate data, LocalDate hoje) {
        BigDecimal v = Dinheiro.exigirPositivo(valor);
        if (data != null && !data.isAfter(hoje)) {
            throw new RegraNegocioException("A data da meta deve ser no futuro");
        }
        metaValor = v;
        metaDescricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
        metaData = data;
    }

    public void removerMeta() {
        metaValor = null;
        metaDescricao = null;
        metaData = null;
    }

    public boolean temMeta() {
        return metaValor != null;
    }

    /** De 0 a 1 (1 = meta alcançada ou passada). */
    public BigDecimal progressoDaMeta() {
        if (!temMeta()) {
            return BigDecimal.ZERO;
        }
        BigDecimal p = getSaldo().divide(metaValor, 4, RoundingMode.DOWN);
        return p.min(BigDecimal.ONE);
    }

    public BigDecimal getMetaValor() {
        return metaValor;
    }

    public String getMetaDescricao() {
        return metaDescricao;
    }

    public LocalDate getMetaData() {
        return metaData;
    }
}
