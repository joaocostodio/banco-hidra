package bancohidra.util;

import bancohidra.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Período de consulta de transações (extrato do ADM e transações do cliente).
 * Sem datas: os últimos 30 dias. No máximo 1 ano. O dia final entra inteiro.
 *
 * @param inicio primeiro instante do período (inclusive)
 * @param fim    primeiro instante depois do período (exclusive)
 */
public record Periodo(LocalDateTime inicio, LocalDateTime fim) {

    public static final int DIAS_PADRAO = 30;
    public static final int DIAS_MAXIMO = 366;

    public static Periodo de(LocalDate inicio, LocalDate fim, LocalDate hoje) {
        LocalDate ate = fim != null ? fim : hoje;
        LocalDate de = inicio != null ? inicio : ate.minusDays(DIAS_PADRAO);
        if (de.isAfter(ate)) {
            throw new RegraNegocioException("A data inicial deve ser anterior à final");
        }
        if (de.plusDays(DIAS_MAXIMO).isBefore(ate)) {
            throw new RegraNegocioException("O período pode ter no máximo 1 ano");
        }
        return new Periodo(de.atStartOfDay(), ate.plusDays(1).atStartOfDay());
    }
}
