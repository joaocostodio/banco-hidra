package bancohidra.util;

import bancohidra.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * "Pix copia e cola": o texto que vai dentro do QR code de cobrança, no padrão BR Code do
 * Banco Central (formato EMV). Cada campo é ID (2 dígitos) + tamanho (2 dígitos) + valor, e o
 * último campo (63) é um CRC16 que detecta código digitado ou colado errado.
 *
 * <p>Exemplo de campos: {@code 00 02 01} (versão), {@code 26 .. } (conta Pix com a chave),
 * {@code 54 05 10.50} (valor), {@code 59 .. } (nome de quem recebe), {@code 63 04 ABCD} (CRC).
 */
public final class BrCode {

    private static final String GUI_PIX = "br.gov.bcb.pix";
    private static final String MOEDA_REAL = "986";

    private BrCode() {
    }

    /** O que vem dentro de um código: a chave é obrigatória; valor e descrição são opcionais. */
    public record Dados(String chave, BigDecimal valor, String descricao, String nomeRecebedor, String cidade,
                        String txid) {
    }

    // ---------- gerar ----------

    public static String gerar(Dados d) {
        StringBuilder conta = new StringBuilder()
                .append(campo("00", GUI_PIX))
                .append(campo("01", d.chave()));
        if (d.descricao() != null && !d.descricao().isBlank()) {
            conta.append(campo("02", limitar(semAcento(d.descricao()), 72)));
        }

        StringBuilder codigo = new StringBuilder()
                .append(campo("00", "01"))
                .append(campo("26", conta.toString()))
                .append(campo("52", "0000"))
                .append(campo("53", MOEDA_REAL));
        if (d.valor() != null) {
            codigo.append(campo("54", Dinheiro.exigirPositivo(d.valor()).toPlainString()));
        }
        codigo.append(campo("58", "BR"))
                .append(campo("59", limitar(semAcento(d.nomeRecebedor()).toUpperCase(Locale.ROOT), 25)))
                .append(campo("60", limitar(semAcento(d.cidade()).toUpperCase(Locale.ROOT), 15)))
                .append(campo("62", campo("05", txidValido(d.txid()))))
                .append("6304");
        return codigo + crc16(codigo.toString());
    }

    // ---------- ler ----------

    /** Lê um código colado pelo pagador. Lança {@link RegraNegocioException} se estiver inválido. */
    public static Dados ler(String texto) {
        String codigo = texto == null ? "" : texto.trim();
        if (codigo.length() < 30 || !codigo.startsWith("000201")) {
            throw new RegraNegocioException("Este não é um código Pix copia e cola");
        }
        int posCrc = codigo.length() - 8;
        if (!codigo.startsWith("6304", posCrc)
                || !crc16(codigo.substring(0, posCrc + 4)).equalsIgnoreCase(codigo.substring(posCrc + 4))) {
            throw new RegraNegocioException("Código Pix incompleto ou alterado. Copie o código inteiro de novo");
        }

        Map<String, String> campos = campos(codigo.substring(0, posCrc));
        Map<String, String> conta = campos(campos.getOrDefault("26", ""));
        if (!GUI_PIX.equalsIgnoreCase(conta.get("00")) || conta.get("01") == null) {
            throw new RegraNegocioException("Este código Pix não tem uma chave");
        }
        BigDecimal valor = null;
        if (campos.get("54") != null) {
            try {
                valor = Dinheiro.exigirPositivo(new BigDecimal(campos.get("54")));
            } catch (NumberFormatException e) {
                throw new RegraNegocioException("Código Pix com valor inválido");
            }
        }
        Map<String, String> adicionais = campos(campos.getOrDefault("62", ""));
        return new Dados(conta.get("01"), valor, conta.get("02"),
                campos.get("59"), campos.get("60"), adicionais.get("05"));
    }

    /** Separa "ID + tamanho + valor" em um mapa. */
    private static Map<String, String> campos(String texto) {
        Map<String, String> campos = new LinkedHashMap<>();
        int i = 0;
        try {
            while (i < texto.length()) {
                String id = texto.substring(i, i + 2);
                int tamanho = Integer.parseInt(texto.substring(i + 2, i + 4));
                campos.put(id, texto.substring(i + 4, i + 4 + tamanho));
                i += 4 + tamanho;
            }
        } catch (RuntimeException formatoQuebrado) {
            throw new RegraNegocioException("Código Pix com formato inválido");
        }
        return campos;
    }

    // ---------- auxiliares ----------

    private static String campo(String id, String valor) {
        if (valor.length() > 99) {
            throw new IllegalArgumentException("Campo " + id + " grande demais");
        }
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** CRC16-CCITT (polinômio 0x1021, início 0xFFFF), em 4 dígitos hexadecimais maiúsculos. */
    static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    /** O identificador da cobrança só aceita letras e números (até 25); sem um, o padrão é "***". */
    private static String txidValido(String txid) {
        String t = txid == null ? "" : txid.replaceAll("[^A-Za-z0-9]", "");
        return t.isEmpty() ? "***" : limitar(t, 25);
    }

    private static String semAcento(String texto) {
        String t = texto == null ? "" : texto.trim();
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
