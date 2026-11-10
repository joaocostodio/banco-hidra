package bancohidra.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfTest {

    @Test
    void aceitaCpfValidoComOuSemPontuacao() {
        assertTrue(Cpf.valido("529.982.247-25"));
        assertTrue(Cpf.valido("52998224725"));
    }

    @Test
    void recusaCpfInvalido() {
        assertFalse(Cpf.valido("529.982.247-26")); // dígito errado
        assertFalse(Cpf.valido("111.111.111-11")); // todos iguais
        assertFalse(Cpf.valido("1234"));
        assertFalse(Cpf.valido(null));
    }

    @Test
    void formataEMascara() {
        assertEquals("529.982.247-25", Cpf.formatar("52998224725"));
        assertEquals("***.982.247-**", Cpf.mascarar("52998224725"));
    }
}
