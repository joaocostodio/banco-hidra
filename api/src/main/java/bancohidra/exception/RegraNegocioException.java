package bancohidra.exception;

/**
 * Violação de uma regra do banco (saldo insuficiente, conta bloqueada, CPF repetido...).
 * Vira resposta HTTP 422 com a mensagem para o usuário.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
