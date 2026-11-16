package bancohidra.security;

import bancohidra.dto.Erro;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;

/**
 * Segunda barreira contra CSRF, além do cookie SameSite=Strict: toda requisição que muda dados
 * (POST, PUT, PATCH, DELETE) precisa do cabeçalho {@value #CABECALHO}.
 *
 * <p>Um formulário ou imagem de outro site não consegue mandar cabeçalhos próprios. Um {@code fetch} de
 * outro site até consegue, mas aí o navegador pergunta antes à API (CORS) e a API não autoriza origens
 * desconhecidas. O site do Banco Hidra manda o cabeçalho em todas as chamadas (auth.interceptor.ts).
 */
@Provider
@PreMatching
public class ProtecaoCsrf implements ContainerRequestFilter {

    public static final String CABECALHO = "X-Requested-With";
    private static final Set<String> SEGUROS = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    @Override
    public void filter(ContainerRequestContext req) {
        if (!SEGUROS.contains(req.getMethod()) && req.getHeaderString(CABECALHO) == null) {
            req.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new Erro("Requisição recusada: origem não confirmada"))
                    .build());
        }
    }
}
