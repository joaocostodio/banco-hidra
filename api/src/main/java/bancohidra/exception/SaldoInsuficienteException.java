package bancohidra.exception;

public class SaldoInsuficienteException extends RegraNegocioException {

    public SaldoInsuficienteException() {
        super("Saldo insuficiente");
    }
}
