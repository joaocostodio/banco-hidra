package bancohidra.util;

/** Validação e formatação de CPF (cálculo oficial dos dois dígitos verificadores). */
public final class Cpf {

    private Cpf() {
    }

    /** Remove tudo que não é dígito: "123.456.789-09" vira "12345678909". */
    public static String somenteDigitos(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false; // tamanho errado ou todos iguais (111.111.111-11)
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    /** Calcula o dígito verificador usando os primeiros {@code tamanho} dígitos. */
    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public static String formatar(String cpf) {
        String d = somenteDigitos(cpf);
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Mostra só o miolo, como os bancos fazem no Pix: ***.456.789-** */
    public static String mascarar(String cpf) {
        String d = somenteDigitos(cpf);
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }
}
