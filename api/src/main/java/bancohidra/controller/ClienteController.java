package bancohidra.controller;

import bancohidra.dto.ClienteDtos.ClienteResponse;
import bancohidra.dto.ClienteDtos.EditarClienteRequest;
import bancohidra.dto.ClienteDtos.NovoClienteRequest;
import bancohidra.entity.Conta;
import bancohidra.entity.Perfil;
import bancohidra.entity.Usuario;
import bancohidra.service.ClienteService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/clientes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClienteController {

    @Inject
    ClienteService clienteService;

    @GET
    public List<ClienteResponse> listar() {
        List<Conta> contas = Conta.list(
                "from Conta c join fetch c.titular t where t.perfil = ?1 order by t.nome", Perfil.CLIENTE);
        return contas.stream().map(c -> ClienteResponse.de(c.titular, c)).toList();
    }

    @GET
    @Path("/{id}")
    public ClienteResponse buscar(@PathParam("id") Long id) {
        Usuario usuario = clienteService.buscar(id);
        return ClienteResponse.de(usuario, Conta.doTitular(usuario));
    }

    /** Cadastra o cliente e abre a conta dele. */
    @POST
    public Response cadastrar(@Valid NovoClienteRequest req) {
        Conta conta = clienteService.cadastrar(req);
        return Response.status(Response.Status.CREATED)
                .entity(ClienteResponse.de(conta.titular, conta)).build();
    }

    @PUT
    @Path("/{id}")
    public ClienteResponse editar(@PathParam("id") Long id, @Valid EditarClienteRequest req) {
        Usuario usuario = clienteService.editar(id, req);
        return ClienteResponse.de(usuario, Conta.doTitular(usuario));
    }
}
