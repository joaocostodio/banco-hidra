package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import bancohidra.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regras do saldo encapsulado: rodam sem banco de dados nem Quarkus. */
class ContaTest {

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    private Conta novaConta() {
        Conta conta = new Conta();
        conta.numero = "000001-6";
        return conta;
    }

    @Test
    void comecaComSaldoZero() {
        assertEquals(reais("0.00"), novaConta().getSaldo());
    }

    @Test
    void depositaESaca() {
        Conta conta = novaConta();
        conta.depositar(reais("100"));
        conta.sacar(reais("30.50"));
        assertEquals(reais("69.50"), conta.getSaldo());
    }

    @Test
    void podeSacarExatamenteOSaldo() {
        Conta conta = novaConta();
        conta.depositar(reais("10"));
        conta.sacar(reais("10"));
        assertEquals(reais("0.00"), conta.getSaldo());
    }

    @Test
    void naoSacaMaisQueOSaldo() {
        Conta conta = novaConta();
        conta.depositar(reais("10"));
        assertThrows(SaldoInsuficienteException.class, () -> conta.sacar(reais("10.01")));
        assertEquals(reais("10.00"), conta.getSaldo(), "saldo não pode mudar quando a operação falha");
    }

    @Test
    void recusaValorZeroNegativoOuComMaisDeDuasCasas() {
        Conta conta = novaConta();
        assertThrows(RegraNegocioException.class, () -> conta.depositar(reais("0")));
        assertThrows(RegraNegocioException.class, () -> conta.depositar(reais("-5")));
        assertThrows(RegraNegocioException.class, () -> conta.depositar(reais("1.005")));
        assertThrows(RegraNegocioException.class, () -> conta.depositar(null));
    }

    @Test
    void contaBloqueadaNaoMovimenta() {
        Conta conta = novaConta();
        conta.depositar(reais("50"));
        conta.bloquear();
        assertThrows(RegraNegocioException.class, () -> conta.depositar(reais("1")));
        assertThrows(RegraNegocioException.class, () -> conta.sacar(reais("1")));
        conta.desbloquear();
        conta.sacar(reais("1"));
        assertEquals(reais("49.00"), conta.getSaldo());
    }

    @Test
    void soEncerraComSaldoZero() {
        Conta conta = novaConta();
        conta.depositar(reais("1"));
        assertThrows(RegraNegocioException.class, conta::encerrar);
        conta.sacar(reais("1"));
        conta.encerrar();
        assertEquals(StatusConta.ENCERRADA, conta.status);
        assertThrows(RegraNegocioException.class, conta::desbloquear);
    }

    @Test
    void geraNumeroComDigitoVerificador() {
        // 000042: 0*7 + 0*6 + 0*5 + 0*4 + 4*3 + 2*2 = 16; 16 % 11 = 5
        assertEquals("000042-5", Conta.gerarNumero(42));
        assertEquals(8, Conta.gerarNumero(123456).length());
    }
}
