package bancohidra.entity;

public enum StatusConta {
    ATIVA,
    /** Não movimenta (nem envia nem recebe) até ser desbloqueada pelo administrador. */
    BLOQUEADA,
    /** Fechada em definitivo; só é possível com saldo zero. */
    ENCERRADA
}
