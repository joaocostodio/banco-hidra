package bancohidra.security;

import java.security.SecureRandom;

/** Gera a senha provisória que o administrador entrega ao cliente. */
public final class SenhaProvisoria {

    // Sem caracteres que se confundem ao ditar ou ler (0/O, 1/l/I)
    private static final String LETRAS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String NUMEROS = "23456789";
    private static final SecureRandom SORTEIO = new SecureRandom();

    private SenhaProvisoria() {
    }

    /** 10 caracteres, sempre com letras e números. */
    public static String gerar() {
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            String fonte = i % 3 == 2 ? NUMEROS : LETRAS;
            senha.append(fonte.charAt(SORTEIO.nextInt(fonte.length())));
        }
        return senha.toString();
    }
}
