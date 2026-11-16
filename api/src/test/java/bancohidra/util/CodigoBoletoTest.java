package bancohidra.util;

import bancohidra.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoBoletoTest {

    private static final String LIVRE = "0001123456789012340000425";

    @Test
    void modulo10ConfereComExemploConhecido() {
        // Campo 1 do boleto de exemplo do Banco do Brasil: 00190.00009 → DV 9
        assertEquals(9, CodigoBoleto.modulo10("001900000"));
    }

    @Test
    void fatorDeVencimentoRecomecaEm1000Em2025() {
        assertEquals("1000", CodigoBoleto.fator(LocalDate.of(2025, 2, 22)));
        assertEquals("1637", CodigoBoleto.fator(LocalDate.of(2026, 11, 21)));
        assertThrows(RegraNegocioException.class, () -> CodigoBoleto.fator(LocalDate.of(2025, 2, 21)));
    }

    @Test
    void montaCodigoDeBarrasELinhaDigitavel() {
        String barras = CodigoBoleto.codigoBarras(LocalDate.of(2026, 11, 21), new BigDecimal("150.75"), LIVRE);

        assertEquals(44, barras.length());
        assertTrue(barras.startsWith("9999"), "banco 999 + moeda 9");
        assertEquals("1637", barras.substring(5, 9), "fator de vencimento");
        assertEquals("0000015075", barras.substring(9, 19), "valor em centavos");
        assertEquals(LIVRE, barras.substring(19));

        String linha = CodigoBoleto.linhaDigitavel(barras);
        assertEquals(47, linha.length());
        assertEquals(barras.charAt(4), linha.charAt(32), "o DV geral aparece no campo 4");
        assertEquals(barras.substring(5, 19), linha.substring(33), "campo 5 = fator + valor");
    }

    @Test
    void leALinhaFormatadaOuOCodigoDeBarras() {
        String barras = CodigoBoleto.codigoBarras(LocalDate.of(2026, 11, 21), new BigDecimal("150.75"), LIVRE);
        String formatada = CodigoBoleto.formatar(CodigoBoleto.linhaDigitavel(barras));

        CodigoBoleto.Lido pelaLinha = CodigoBoleto.ler(formatada);
        assertEquals(barras, pelaLinha.codigoBarras());
        assertEquals(LocalDate.of(2026, 11, 21), pelaLinha.vencimento());
        assertEquals(new BigDecimal("150.75"), pelaLinha.valor());

        assertEquals(barras, CodigoBoleto.ler(barras).codigoBarras());
    }

    @Test
    void pegaErroDeDigitacao() {
        String barras = CodigoBoleto.codigoBarras(LocalDate.of(2026, 11, 21), new BigDecimal("150.75"), LIVRE);
        String linha = CodigoBoleto.linhaDigitavel(barras);

        for (int i : new int[] {2, 12, 25, 40}) {
            char trocado = (char) ('0' + (linha.charAt(i) - '0' + 1) % 10);
            String errada = linha.substring(0, i) + trocado + linha.substring(i + 1);
            assertThrows(RegraNegocioException.class, () -> CodigoBoleto.ler(errada), "posição " + i);
        }
        assertThrows(RegraNegocioException.class, () -> CodigoBoleto.ler(linha.substring(1)));
        assertThrows(RegraNegocioException.class, () -> CodigoBoleto.ler("abc"));
    }
}
