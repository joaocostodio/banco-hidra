package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras do boleto que não precisam do banco de dados: encargos e mudança de status. */
class BoletoTest {

    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 11, 20);

    private static Boleto boleto(String valor) {
        Boleto b = new Boleto();
        b.valor = new BigDecimal(valor);
        b.vencimento = VENCIMENTO;
        return b;
    }

    @Test
    void ateOVencimentoNaoTemEncargos() {
        Boleto b = boleto("100.00");
        assertEquals(new BigDecimal("100.00"), b.encargosEm(VENCIMENTO.minusDays(3)).total());
        assertEquals(new BigDecimal("100.00"), b.encargosEm(VENCIMENTO).total());
        assertFalse(b.vencidoEm(VENCIMENTO));
    }

    @Test
    void depoisDoVencimentoCobraMultaEJurosPorDia() {
        Boleto b = boleto("100.00");
        Boleto.Encargos e = b.encargosEm(VENCIMENTO.plusDays(10));

        assertEquals(10, e.diasAtraso());
        assertEquals(new BigDecimal("2.00"), e.multa());             // 2%
        assertEquals(new BigDecimal("0.33"), e.juros());             // 1% ao mês × 10/30
        assertEquals(new BigDecimal("102.33"), e.total());
        assertTrue(b.vencidoEm(VENCIMENTO.plusDays(1)));
    }

    @Test
    void umMesDeAtrasoDaUmPorCentoDeJuros() {
        Boleto.Encargos e = boleto("250.00").encargosEm(VENCIMENTO.plusDays(30));
        assertEquals(new BigDecimal("5.00"), e.multa());
        assertEquals(new BigDecimal("2.50"), e.juros());
        assertEquals(new BigDecimal("257.50"), e.total());
    }

    @Test
    void soPagaOuCancelaUmaVez() {
        Boleto pago = boleto("10.00");
        pago.registrarPagamento(new ContaCorrente(), new BigDecimal("10.00"), UUID.randomUUID(), LocalDateTime.now());
        assertEquals(StatusBoleto.PAGO, pago.getStatus());
        assertThrows(RegraNegocioException.class,
                () -> pago.registrarPagamento(new ContaCorrente(), BigDecimal.TEN, UUID.randomUUID(), LocalDateTime.now()));
        assertThrows(RegraNegocioException.class, pago::cancelar);

        Boleto cancelado = boleto("10.00");
        cancelado.cancelar();
        assertEquals(StatusBoleto.CANCELADO, cancelado.getStatus());
        assertFalse(cancelado.vencidoEm(VENCIMENTO.plusDays(5)), "cancelado não fica 'vencido'");
        assertThrows(RegraNegocioException.class, cancelado::exigirAberto);
    }

    @Test
    void emissaoValidaVencimento() {
        Conta conta = new ContaCorrente();
        conta.numero = "000042-5";
        LocalDate hoje = LocalDate.of(2026, 11, 16);
        assertThrows(RegraNegocioException.class,
                () -> Boleto.emitir(conta, BigDecimal.TEN, hoje.minusDays(1), null, hoje));
        assertThrows(RegraNegocioException.class,
                () -> Boleto.emitir(conta, BigDecimal.TEN, hoje.plusDays(Boleto.PRAZO_MAXIMO_DIAS + 1), null, hoje));
    }
}
