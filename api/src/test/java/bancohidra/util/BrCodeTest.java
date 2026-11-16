package bancohidra.util;

import bancohidra.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrCodeTest {

    @Test
    void crcConfereComOValorDeReferencia() {
        // CRC16-CCITT (0xFFFF) de "123456789" é 29B1: valor de referência do algoritmo
        assertEquals("29B1", BrCode.crc16("123456789"));
    }

    @Test
    void geraELeDeVolta() {
        String codigo = BrCode.gerar(new BrCode.Dados("ana@hidra.com", new BigDecimal("25.5"), "Pizza de sábado",
                "Ana Clara de Souza Pereira Lima", "Três de Maio", "HIDRA123"));

        assertTrue(codigo.startsWith("000201"));
        assertTrue(codigo.contains("5802BR"));
        assertTrue(codigo.contains("540525.50"), "valor com 2 casas");

        BrCode.Dados lido = BrCode.ler(codigo);
        assertEquals("ana@hidra.com", lido.chave());
        assertEquals(new BigDecimal("25.50"), lido.valor());
        assertEquals("Pizza de sabado", lido.descricao(), "sem acento, como pede o padrão");
        assertEquals("ANA CLARA DE SOUZA PEREIR", lido.nomeRecebedor(), "nome cortado em 25 caracteres");
        assertEquals(25, lido.nomeRecebedor().length());
        assertEquals("TRES DE MAIO", lido.cidade());
        assertEquals("HIDRA123", lido.txid());
    }

    @Test
    void semValorQuemPagaEscolhe() {
        String codigo = BrCode.gerar(new BrCode.Dados("+5555999990000", null, null, "Beto", "Tres de Maio", null));
        BrCode.Dados lido = BrCode.ler(codigo);
        assertNull(lido.valor());
        assertNull(lido.descricao());
        assertEquals("***", lido.txid());
    }

    @Test
    void recusaCodigoAlteradoOuQuebrado() {
        String codigo = BrCode.gerar(new BrCode.Dados("ana@hidra.com", new BigDecimal("10.00"), null, "Ana",
                "Tres de Maio", null));

        assertThrows(RegraNegocioException.class, () -> BrCode.ler(codigo.replace("10.00", "99.00")));
        assertThrows(RegraNegocioException.class, () -> BrCode.ler(codigo.substring(0, codigo.length() - 3)));
        assertThrows(RegraNegocioException.class, () -> BrCode.ler("ana@hidra.com"));
        assertThrows(RegraNegocioException.class, () -> BrCode.ler(null));
    }
}
