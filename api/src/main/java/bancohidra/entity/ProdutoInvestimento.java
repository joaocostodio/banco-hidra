package bancohidra.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Produtos de investimento do Banco Hidra. As taxas de referência são <b>fictícias</b> (fixas no código, para o
 * projeto não depender de uma fonte externa): CDI de {@value #CDI_TEXTO} e Selic de {@value #SELIC_TEXTO} ao ano.
 */
public enum ProdutoInvestimento {

    CDB("CDB 110% do CDI", new BigDecimal("1.10"), Indice.CDI, false),
    TESOURO_SELIC("Tesouro Selic", BigDecimal.ONE, Indice.SELIC, false),
    LCI("LCI 95% do CDI (isenta de IR)", new BigDecimal("0.95"), Indice.CDI, true);

    static final String CDI_TEXTO = "10,65%";
    static final String SELIC_TEXTO = "10,75%";

    enum Indice {
        CDI(new BigDecimal("0.1065")),
        SELIC(new BigDecimal("0.1075"));

        final BigDecimal taxaAnual;

        Indice(BigDecimal taxaAnual) {
            this.taxaAnual = taxaAnual;
        }
    }

    private final String nome;
    private final BigDecimal percentualDoIndice;
    private final Indice indice;
    private final boolean isentoIR;

    ProdutoInvestimento(String nome, BigDecimal percentualDoIndice, Indice indice, boolean isentoIR) {
        this.nome = nome;
        this.percentualDoIndice = percentualDoIndice;
        this.indice = indice;
        this.isentoIR = isentoIR;
    }

    public String getNome() {
        return nome;
    }

    /** Taxa ao ano, ex.: 0.117150 (11,715%) para o CDB 110% do CDI. */
    public BigDecimal taxaAnual() {
        return indice.taxaAnual.multiply(percentualDoIndice).setScale(6, RoundingMode.HALF_EVEN);
    }

    public boolean isIsentoIR() {
        return isentoIR;
    }
}
