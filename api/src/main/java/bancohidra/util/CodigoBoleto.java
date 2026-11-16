package bancohidra.util;

import bancohidra.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Código de barras (44 dígitos) e linha digitável (47 dígitos) de boleto, no padrão Febraban.
 *
 * <pre>
 * Código de barras: BBB M D FFFF VVVVVVVVVV LLLLLLLLLLLLLLLLLLLLLLLLL
 *                   banco, moeda (9 = real), DV geral (módulo 11), fator de vencimento,
 *                   valor em centavos e 25 dígitos livres (o banco usa como quiser).
 *
 * Linha digitável:  campo 1 (banco + moeda + 5 livres + DV), campo 2 (10 livres + DV),
 *                   campo 3 (10 livres + DV), campo 4 (DV geral), campo 5 (fator + valor).
 *                   Os DVs dos campos 1 a 3 são módulo 10: pegam erro de digitação.
 * </pre>
 */
public final class CodigoBoleto {

    /** Código fictício do Banco Hidra na "compensação". */
    public static final String BANCO_HIDRA = "999";
    private static final String MOEDA_REAL = "9";

    /**
     * O fator de vencimento chegou a 9999 em 21/02/2025 e recomeçou em 1000 no dia seguinte
     * (regra da Febraban). Assim cada fator volta a valer por mais ~24 anos.
     */
    private static final LocalDate DATA_FATOR_1000 = LocalDate.of(2025, 2, 22);
    private static final long VALOR_MAXIMO_CENTAVOS = 9_999_999_999L;

    private CodigoBoleto() {
    }

    /** O que se descobre só lendo os números, sem consultar o banco. */
    public record Lido(String codigoBarras, LocalDate vencimento, BigDecimal valor) {
    }

    public static String codigoBarras(LocalDate vencimento, BigDecimal valor, String campoLivre) {
        if (!campoLivre.matches("\\d{25}")) {
            throw new IllegalArgumentException("O campo livre deve ter 25 dígitos");
        }
        long centavos = Dinheiro.exigirPositivo(valor).movePointRight(2).longValueExact();
        if (centavos > VALOR_MAXIMO_CENTAVOS) {
            throw new RegraNegocioException("Valor alto demais para um boleto");
        }
        String semDv = BANCO_HIDRA + MOEDA_REAL + fator(vencimento) + String.format("%010d", centavos) + campoLivre;
        return semDv.substring(0, 4) + dvGeral(semDv) + semDv.substring(4);
    }

    public static String linhaDigitavel(String codigoBarras) {
        String livre = codigoBarras.substring(19);
        String c1 = codigoBarras.substring(0, 4) + livre.substring(0, 5);
        String c2 = livre.substring(5, 15);
        String c3 = livre.substring(15, 25);
        return c1 + modulo10(c1) + c2 + modulo10(c2) + c3 + modulo10(c3)
                + codigoBarras.charAt(4) + codigoBarras.substring(5, 19);
    }

    /** 47 dígitos → "99990.00012 34567.890123 45678.901234 5 10000000012345". */
    public static String formatar(String linha) {
        return linha.substring(0, 5) + "." + linha.substring(5, 10) + " "
                + linha.substring(10, 15) + "." + linha.substring(15, 21) + " "
                + linha.substring(21, 26) + "." + linha.substring(26, 32) + " "
                + linha.charAt(32) + " " + linha.substring(33);
    }

    /**
     * Lê o que o pagador digitou ou colou: a linha digitável (47 dígitos) ou o código de barras (44).
     * Pontos e espaços são ignorados. Confere todos os dígitos verificadores.
     */
    public static Lido ler(String digitado) {
        String d = digitado == null ? "" : digitado.replaceAll("[\\s.\\-]", "");
        if (!d.matches("\\d+")) {
            throw new RegraNegocioException("A linha digitável só tem números");
        }
        String barras;
        if (d.length() == 47) {
            if (modulo10(d.substring(0, 9)) != d.charAt(9) - '0'
                    || modulo10(d.substring(10, 20)) != d.charAt(20) - '0'
                    || modulo10(d.substring(21, 31)) != d.charAt(31) - '0') {
                throw new RegraNegocioException("Linha digitável inválida: confira os números digitados");
            }
            barras = d.substring(0, 4) + d.charAt(32) + d.substring(33, 47)
                    + d.substring(4, 9) + d.substring(10, 20) + d.substring(21, 31);
        } else if (d.length() == 44) {
            barras = d;
        } else {
            throw new RegraNegocioException("A linha digitável tem 47 números (você digitou " + d.length() + ")");
        }
        if (dvGeral(barras.substring(0, 4) + barras.substring(5)) != barras.charAt(4) - '0') {
            throw new RegraNegocioException("Código do boleto inválido: confira os números digitados");
        }
        LocalDate vencimento = DATA_FATOR_1000.plusDays(Integer.parseInt(barras.substring(5, 9)) - 1000L);
        BigDecimal valor = new BigDecimal(Long.parseLong(barras.substring(9, 19))).movePointLeft(2);
        return new Lido(barras, vencimento, valor);
    }

    static String fator(LocalDate vencimento) {
        long dias = ChronoUnit.DAYS.between(DATA_FATOR_1000, vencimento);
        if (dias < 0 || dias > 8999) {
            throw new RegraNegocioException("Data de vencimento fora do intervalo aceito");
        }
        return String.valueOf(1000 + dias);
    }

    /** Módulo 10: pesos 2, 1, 2, 1... da direita para a esquerda; produto ≥ 10 soma os dígitos. */
    static int modulo10(String numeros) {
        int soma = 0;
        int peso = 2;
        for (int i = numeros.length() - 1; i >= 0; i--) {
            int p = (numeros.charAt(i) - '0') * peso;
            soma += p > 9 ? p - 9 : p;
            peso = peso == 2 ? 1 : 2;
        }
        return (10 - soma % 10) % 10;
    }

    /** DV geral (módulo 11): pesos 2 a 9 da direita para a esquerda; resultado 0, 10 ou 11 vira 1. */
    static int dvGeral(String numeros43) {
        int soma = 0;
        int peso = 2;
        for (int i = numeros43.length() - 1; i >= 0; i--) {
            soma += (numeros43.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int dv = 11 - soma % 11;
        return dv == 0 || dv == 10 || dv == 11 ? 1 : dv;
    }
}
