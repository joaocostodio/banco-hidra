package bancohidra.entity;

/** Vencido não é um status: é um boleto ABERTO com o vencimento no passado (aí cobra multa e juros). */
public enum StatusBoleto {
    ABERTO,
    PAGO,
    CANCELADO
}
