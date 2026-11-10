package bancohidra.exception;

import bancohidra.dto.Erro;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Converte as exceções do domínio em respostas JSON no formato {"erro": "..."}. */
public class TratadorDeErros {

    @ServerExceptionMapper
    public Response regraNegocio(RegraNegocioException e) {
        return Response.status(422).entity(new Erro(e.getMessage())).build();
    }

    @ServerExceptionMapper
    public Response naoEncontrado(NaoEncontradoException e) {
        return Response.status(Response.Status.NOT_FOUND).entity(new Erro(e.getMessage())).build();
    }
}
