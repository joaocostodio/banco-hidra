package bancohidra.service;

import bancohidra.entity.TipoChavePix;
import bancohidra.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChavePixNormalizacaoTest {

    @Test
    void normalizaCadaTipo() {
        assertEquals("52998224725", ChavePixService.normalizar(TipoChavePix.CPF, "529.982.247-25"));
        assertEquals("ana@hidra.com", ChavePixService.normalizar(TipoChavePix.EMAIL, "  Ana@Hidra.COM "));
        assertEquals("+5555999990000", ChavePixService.normalizar(TipoChavePix.TELEFONE, "(55) 99999-0000"));
        assertEquals("+5555999990000", ChavePixService.normalizar(TipoChavePix.TELEFONE, "+55 55 99999-0000"));
    }

    @Test
    void recusaFormatosErrados() {
        assertThrows(RegraNegocioException.class, () -> ChavePixService.normalizar(TipoChavePix.CPF, "123"));
        assertThrows(RegraNegocioException.class, () -> ChavePixService.normalizar(TipoChavePix.EMAIL, "ana"));
        assertThrows(RegraNegocioException.class, () -> ChavePixService.normalizar(TipoChavePix.TELEFONE, "3333-4444"));
        assertThrows(RegraNegocioException.class, () -> ChavePixService.normalizar(TipoChavePix.ALEATORIA, "xyz"));
    }
}
