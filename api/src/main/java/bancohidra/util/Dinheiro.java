package bancohidra.util;

import bancohidra.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras comuns para valores em reais. */
public final class Dinheiro {

    private Dinheiro() {
    }

    /** Valida que o valor é positivo e tem no máximo 2 casas decimais; devolve com escala 2. */
    public static BigDecimal exigirPositivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("O valor deve ser maior que zero");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException("O valor deve ter no máximo 2 casas decimais");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }
}
