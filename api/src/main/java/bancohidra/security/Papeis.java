package bancohidra.security;

/**
 * Papéis (groups) gravados no token. ADMIN e CLIENTE vêm do {@link bancohidra.entity.Perfil}.
 * Quem ainda está com a senha provisória recebe só TROCAR_SENHA: com ele o único caminho
 * liberado é a troca de senha; depois disso um token novo, com o papel real, é emitido.
 */
public final class Papeis {

    public static final String ADMIN = "ADMIN";
    public static final String CLIENTE = "CLIENTE";
    public static final String TROCAR_SENHA = "TROCAR_SENHA";

    private Papeis() {
    }
}
