package bancohidra.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PainelDtos {

    private PainelDtos() {
    }

    /**
     * Números do painel inicial do administrador.
     *
     * <p>Uma "operação" é um depósito, uma transferência ou um Pix, contado uma vez só
     * (a transferência tem duas linhas de extrato, mas é uma operação).</p>
     */
    public record ResumoResponse(
            BigDecimal totalCustodiado,
            long clientes,
            long clientesNoMes,
            long primeiroAcessoPendente,
            long contasAtivas,
            long contasBloqueadas,
            long contasEncerradas,
            long operacoesHoje,
            BigDecimal valorHoje,
            List<Dia> ultimosDias) {
    }

    public record Dia(LocalDate data, long operacoes, BigDecimal valor) {
    }
}
