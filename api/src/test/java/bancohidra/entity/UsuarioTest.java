package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {

    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 9, 10, 0);

    @Test
    void exigeSenhaForte() {
        Usuario u = new Usuario();
        assertThrows(RegraNegocioException.class, () -> u.definirSenha("curta1"));
        assertThrows(RegraNegocioException.class, () -> u.definirSenha("somenteletras"));
        assertThrows(RegraNegocioException.class, () -> u.definirSenha("12345678"));
        u.definirSenha("letras123");
        assertTrue(u.autenticar("letras123", agora));
    }

    @Test
    void senhaProvisoriaObrigaTroca() {
        Usuario u = new Usuario();
        u.definirSenhaProvisoria("Ab2Cd3Ef4G");
        assertTrue(u.isTrocarSenha());
        u.definirSenha("novaSenha1");
        assertFalse(u.isTrocarSenha());
    }

    @Test
    void bloqueiaPor15MinutosNaQuintaFalha() {
        Usuario u = new Usuario();
        u.definirSenha("certa1234");
        for (int i = 0; i < 4; i++) {
            assertFalse(u.autenticar("errada", agora));
            assertFalse(u.acessoBloqueado(agora));
        }
        assertFalse(u.autenticar("errada", agora));
        assertTrue(u.acessoBloqueado(agora));
        assertFalse(u.autenticar("certa1234", agora.plusMinutes(14)), "bloqueado mesmo com a senha certa");
        assertTrue(u.autenticar("certa1234", agora.plusMinutes(15)), "liberado depois de 15 minutos");
    }

    @Test
    void acertoZeraAsFalhas() {
        Usuario u = new Usuario();
        u.definirSenha("certa1234");
        for (int i = 0; i < 4; i++) {
            u.autenticar("errada", agora);
        }
        assertTrue(u.autenticar("certa1234", agora));
        assertFalse(u.autenticar("errada", agora));
        assertFalse(u.acessoBloqueado(agora), "a contagem recomeçou depois do acerto");
    }
}
