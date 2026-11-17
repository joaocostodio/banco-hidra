package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * Investimento num produto (CDB, Tesouro Selic, LCI). Rende todo dia, com juros compostos pela taxa anual do produto.
 *
 * <p>Regras que mudam em relação a uma conta comum (sobrescrevendo os ganchos de {@link Conta}):
 * <ul>
 *   <li>só recebe dinheiro (aplicação) e só manda dinheiro (resgate) de/para contas do mesmo titular;</li>
 *   <li>não aceita lançamento manual de receita ou despesa;</li>
 *   <li>no resgate desconta o Imposto de Renda sobre o lucro, pela tabela regressiva (menos imposto quanto mais
 *       tempo o dinheiro ficou aplicado), salvo produto isento.</li>
 * </ul>
 * Para separar o lucro do dinheiro aplicado, guarda o {@code valorAplicado} e a data média das aplicações.
 */
@Entity
@DiscriminatorValue("INVESTIMENTO")
public class ContaInvestimento extends ContaRemunerada {

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private ProdutoInvestimento produto;

    /** Quanto do saldo é dinheiro aplicado (o resto é lucro). */
    @Column(name = "valor_aplicado", precision = 14, scale = 2)
    private BigDecimal valorAplicado;

    /** Data média das aplicações, ponderada pelo valor (base do prazo do IR). */
    @Column(name = "aplicado_desde")
    private LocalDate aplicadoDesde;

    protected ContaInvestimento() {
        // para o JPA
    }

    public ContaInvestimento(ProdutoInvestimento produto) {
        if (produto == null) {
            throw new RegraNegocioException("Escolha o produto do investimento");
        }
        this.produto = produto;
        this.nome = produto == ProdutoInvestimento.TESOURO_SELIC ? "Tesouro Selic" : produto.name();
    }

    @Override
    public TipoConta getTipo() {
        return TipoConta.INVESTIMENTO;
    }

    public ProdutoInvestimento getProduto() {
        return produto != null ? produto : ProdutoInvestimento.CDB;
    }

    public BigDecimal getValorAplicado() {
        return valorAplicado != null ? valorAplicado : BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal getLucro() {
        return getSaldo().subtract(getValorAplicado()).max(BigDecimal.ZERO);
    }

    // ---------- rendimento diário ----------

    /** saldo × ((1 + taxa anual)^(dias/365) − 1). Se ainda não deu 1 centavo, acumula para o próximo dia. */
    @Override
    protected Rendimento calcularRendimento(LocalDate desde, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(desde, hoje);
        if (dias < 1 || getSaldo().signum() == 0) {
            return new Rendimento(BigDecimal.ZERO, getSaldo().signum() == 0 ? hoje : desde);
        }
        double fator = Math.pow(1 + getProduto().taxaAnual().doubleValue(), dias / 365.0) - 1;
        BigDecimal valor = getSaldo().multiply(BigDecimal.valueOf(fator)).setScale(2, RoundingMode.HALF_EVEN);
        return valor.signum() > 0 ? new Rendimento(valor, hoje) : new Rendimento(BigDecimal.ZERO, desde);
    }

    @Override
    public String descricaoRendimento() {
        return "Rendimento: " + getProduto().getNome();
    }

    @Override
    public LocalDate proximoRendimento(LocalDate hoje) {
        return hoje.plusDays(1);
    }

    @Override
    public void recuarRelogio(Period periodo) {
        super.recuarRelogio(periodo);
        if (aplicadoDesde != null) {
            aplicadoDesde = aplicadoDesde.minus(periodo); // o tempo aplicado também conta para o IR
        }
    }

    // ---------- regras de movimentação ----------

    @Override
    public void exigirPodeReceberDe(Conta origem) {
        if (origem != null && !mesmoTitularQue(origem)) {
            throw new RegraNegocioException("O investimento só recebe aplicações das suas próprias contas");
        }
    }

    @Override
    public void exigirPodeEnviarPara(Conta destino) {
        if (!mesmoTitularQue(destino)) {
            throw new RegraNegocioException("O resgate do investimento só pode ir para uma conta sua");
        }
    }

    @Override
    public boolean aceitaLancamento() {
        return false;
    }

    /** Aplicação: soma ao valor aplicado e anda a data média proporcionalmente ao valor novo. */
    @Override
    public void aoReceber(BigDecimal valor, LocalDate hoje) {
        BigDecimal aplicado = getValorAplicado();
        if (aplicadoDesde == null || aplicado.signum() == 0) {
            aplicadoDesde = hoje;
        } else {
            long dias = ChronoUnit.DAYS.between(aplicadoDesde, hoje);
            BigDecimal peso = valor.divide(aplicado.add(valor), 6, RoundingMode.HALF_EVEN);
            aplicadoDesde = aplicadoDesde.plusDays(Math.round(dias * peso.doubleValue()));
        }
        valorAplicado = aplicado.add(valor);
    }

    /**
     * Resgate: a parte do valor que é lucro (na mesma proporção do saldo) paga IR; o resto devolve dinheiro
     * aplicado. Devolve o imposto, que sai da conta junto com o resgate.
     */
    @Override
    public BigDecimal prepararSaida(BigDecimal valor, LocalDate hoje) {
        BigDecimal saldo = getSaldo();
        if (saldo.signum() == 0 || valor.compareTo(saldo) > 0) {
            return BigDecimal.ZERO.setScale(2); // o saque vai recusar por saldo insuficiente
        }
        BigDecimal lucroResgatado = getLucro().multiply(valor).divide(saldo, 2, RoundingMode.HALF_EVEN);
        valorAplicado = getValorAplicado().subtract(valor.subtract(lucroResgatado)).max(BigDecimal.ZERO);
        return impostoSobre(lucroResgatado, hoje);
    }

    /** Imposto que seria descontado se resgatasse tudo hoje (para a tela). */
    public BigDecimal irSeResgatarTudo(LocalDate hoje) {
        return impostoSobre(getLucro(), hoje);
    }

    public BigDecimal aliquotaIR(LocalDate hoje) {
        if (getProduto().isIsentoIR()) {
            return BigDecimal.ZERO;
        }
        long dias = aplicadoDesde == null ? 0 : ChronoUnit.DAYS.between(aplicadoDesde, hoje);
        return aliquotaPorPrazo(dias);
    }

    /** Tabela regressiva do IR de renda fixa. */
    public static BigDecimal aliquotaPorPrazo(long dias) {
        if (dias <= 180) {
            return new BigDecimal("0.225");
        }
        if (dias <= 360) {
            return new BigDecimal("0.200");
        }
        if (dias <= 720) {
            return new BigDecimal("0.175");
        }
        return new BigDecimal("0.150");
    }

    private BigDecimal impostoSobre(BigDecimal lucro, LocalDate hoje) {
        return lucro.multiply(aliquotaIR(hoje)).setScale(2, RoundingMode.HALF_EVEN);
    }
}
