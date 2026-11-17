package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras da poupança e do investimento (herança e polimorfismo), sem banco de dados. */
class ContaRemuneradaTest {

    private static final LocalDate ABERTURA = LocalDate.of(2026, 1, 10);

    private static Usuario titular(long id) {
        Usuario u = new Usuario();
        u.id = id;
        return u;
    }

    private static <T extends Conta> T aberta(T conta, long titularId) {
        conta.titular = titular(titularId);
        conta.numero = "000001-9";
        conta.abertaEm = ABERTURA.atStartOfDay();
        return conta;
    }

    private static BigDecimal reais(String v) {
        return new BigDecimal(v);
    }

    // ---------- fábrica e polimorfismo ----------

    @Test
    void fabricaCriaASubclasseDeCadaTipo() {
        assertTrue(Conta.nova(TipoConta.CORRENTE, null) instanceof ContaCorrente);
        assertTrue(Conta.nova(TipoConta.CARTEIRA, null) instanceof ContaCarteira);
        assertTrue(Conta.nova(TipoConta.POUPANCA, null) instanceof ContaPoupanca);
        assertTrue(Conta.nova(TipoConta.INVESTIMENTO, ProdutoInvestimento.CDB) instanceof ContaInvestimento);
        assertThrows(RegraNegocioException.class, () -> Conta.nova(TipoConta.INVESTIMENTO, null));
    }

    @Test
    void contaComumNaoTemImpostoEAceitaLancamento() {
        Conta corrente = aberta(new ContaCorrente(), 1);
        corrente.depositar(reais("100.00"));
        assertEquals(reais("0.00"), corrente.prepararSaida(reais("50.00"), ABERTURA));
        assertTrue(corrente.aceitaLancamento());
    }

    // ---------- poupança ----------

    @Test
    void poupancaRendeMeioPorCentoNoAniversario() {
        ContaPoupanca p = aberta(new ContaPoupanca(), 1);
        p.depositar(reais("1000.00"));

        assertEquals(reais("0.00"), p.render(ABERTURA.plusDays(20)), "antes do aniversário não rende");
        assertEquals(reais("5.00"), p.render(ABERTURA.plusMonths(1)), "0,5% de 1.000");
        assertEquals(reais("1005.00"), p.getSaldo());
        assertEquals(reais("0.00"), p.render(ABERTURA.plusMonths(1)), "não paga duas vezes o mesmo mês");
        assertEquals(ABERTURA.plusMonths(2), p.proximoRendimento(ABERTURA.plusMonths(1).plusDays(1)));
    }

    @Test
    void poupancaParadaDoisMesesRendeComposto() {
        ContaPoupanca p = aberta(new ContaPoupanca(), 1);
        p.depositar(reais("1000.00"));
        // 1000 × (1,005² − 1) = 10,025 → 10,02 (arredondamento bancário)
        assertEquals(reais("10.02"), p.render(ABERTURA.plusMonths(2)));
    }

    @Test
    void metaDaPoupanca() {
        ContaPoupanca p = aberta(new ContaPoupanca(), 1);
        p.depositar(reais("250.00"));
        p.definirMeta(reais("1000.00"), "Viagem", ABERTURA.plusMonths(6), ABERTURA);
        assertEquals(new BigDecimal("0.2500"), p.progressoDaMeta());
        assertThrows(RegraNegocioException.class,
                () -> p.definirMeta(reais("1000.00"), null, ABERTURA.minusDays(1), ABERTURA), "data no passado");
        p.removerMeta();
        assertFalse(p.temMeta());
    }

    // ---------- investimento ----------

    @Test
    void investimentoRendeTodoDiaPelaTaxaDoProduto() {
        ContaInvestimento cdb = aberta(new ContaInvestimento(ProdutoInvestimento.CDB), 1);
        cdb.depositar(reais("10000.00"));
        cdb.aoReceber(reais("10000.00"), ABERTURA);

        // CDB 110% do CDI (10,65%) = 11,715% ao ano; 30 dias: 10.000 × (1,11715^(30/365) − 1) ≈ 91,40
        BigDecimal rendeu = cdb.render(ABERTURA.plusDays(30));
        assertTrue(rendeu.compareTo(reais("91.00")) > 0 && rendeu.compareTo(reais("92.00")) < 0, "rendeu " + rendeu);
        assertEquals(reais("10000.00"), cdb.getValorAplicado(), "rendimento é lucro, não aplicação");
        assertEquals(rendeu, cdb.getLucro());
    }

    @Test
    void resgateDescontaIrSoSobreOLucroPelaTabelaRegressiva() {
        ContaInvestimento cdb = aberta(new ContaInvestimento(ProdutoInvestimento.CDB), 1);
        cdb.depositar(reais("1000.00"));
        cdb.aoReceber(reais("1000.00"), ABERTURA);
        simularRendimento(cdb, reais("100.00")); // saldo 1.100: 1.000 aplicados + 100 de lucro

        // Resgata metade (550): metade é lucro (50); até 180 dias o IR é 22,5% → 11,25
        assertEquals(reais("11.25"), cdb.prepararSaida(reais("550.00"), ABERTURA.plusDays(100)));
        assertEquals(reais("500.00"), cdb.getValorAplicado(), "saiu 500 do dinheiro aplicado");
    }

    @Test
    void tabelaRegressivaDoIr() {
        assertEquals(new BigDecimal("0.225"), ContaInvestimento.aliquotaPorPrazo(180));
        assertEquals(new BigDecimal("0.200"), ContaInvestimento.aliquotaPorPrazo(181));
        assertEquals(new BigDecimal("0.175"), ContaInvestimento.aliquotaPorPrazo(500));
        assertEquals(new BigDecimal("0.150"), ContaInvestimento.aliquotaPorPrazo(721));
    }

    @Test
    void lciNaoPagaIr() {
        ContaInvestimento lci = aberta(new ContaInvestimento(ProdutoInvestimento.LCI), 1);
        lci.depositar(reais("1000.00"));
        lci.aoReceber(reais("1000.00"), ABERTURA);
        simularRendimento(lci, reais("100.00"));
        assertEquals(reais("0.00"), lci.prepararSaida(reais("1100.00"), ABERTURA.plusDays(10)));
    }

    @Test
    void investimentoSoMovimentaComOMesmoTitular() {
        ContaInvestimento inv = aberta(new ContaInvestimento(ProdutoInvestimento.TESOURO_SELIC), 1);
        Conta minha = aberta(new ContaCorrente(), 1);
        Conta deOutro = aberta(new ContaCorrente(), 2);

        inv.exigirPodeReceberDe(minha);
        inv.exigirPodeEnviarPara(minha);
        assertThrows(RegraNegocioException.class, () -> inv.exigirPodeReceberDe(deOutro));
        assertThrows(RegraNegocioException.class, () -> inv.exigirPodeEnviarPara(deOutro));
        assertFalse(inv.aceitaLancamento());
    }

    @Test
    void simularUmMesAntecipaRendimentoEPrazoDoIr() {
        ContaInvestimento cdb = aberta(new ContaInvestimento(ProdutoInvestimento.CDB), 1);
        cdb.depositar(reais("1000.00"));
        cdb.aoReceber(reais("1000.00"), LocalDate.now());
        cdb.render(LocalDate.now());

        for (int i = 0; i < 7; i++) {
            cdb.recuarRelogio(Period.ofMonths(1));
            cdb.render(LocalDate.now());
        }
        // ~7 meses aplicados: passou dos 180 dias, a alíquota caiu para 20%
        assertEquals(new BigDecimal("0.200"), cdb.aliquotaIR(LocalDate.now()));
        assertTrue(cdb.getLucro().signum() > 0);
    }

    /** Põe lucro na conta como se tivesse rendido (render usa a data; aqui só interessa o valor). */
    private static void simularRendimento(ContaRemunerada conta, BigDecimal valor) {
        conta.abertaEm = LocalDateTime.of(2000, 1, 1, 0, 0);
        conta.ultimoRendimento = null;
        // credita pelo mesmo caminho do rendimento real
        conta.adicionarRendimento(valor);
    }
}
