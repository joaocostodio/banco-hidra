package bancohidra.exception;

/** Login recusado. Vira resposta HTTP 401. */
public class NaoAutorizadoException extends RuntimeException {

    public NaoAutorizadoException(String mensagem) {
        super(mensagem);
    }
}
