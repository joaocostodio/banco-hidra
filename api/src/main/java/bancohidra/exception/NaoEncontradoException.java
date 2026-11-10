package bancohidra.exception;

/** Registro pedido não existe. Vira resposta HTTP 404. */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
