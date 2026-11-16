package bancohidra.controller;

import bancohidra.dto.ClienteDtos.ClienteResponse;
import bancohidra.dto.ClienteDtos.EditarClienteRequest;
import bancohidra.dto.ClienteDtos.NovoClienteRequest;
import bancohidra.dto.ClienteDtos.NovoClienteResponse;
import bancohidra.dto.ClienteDtos.SenhaProvisoriaResponse;
import bancohidra.entity.Conta;
import bancohidra.entity.Perfil;
import bancohidra.entity.Usuario;
import bancohidra.security.Papeis;
import bancohidra.service.ClienteService;
import jakarta.annotation.security.RolesAllowed;
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

/** Gestão de clientes: exclusiva do administrador. */
@Path("/clientes")
@RolesAllowed(Papeis.ADMIN)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClienteController {

    @Inject
    ClienteService clienteService;

    @GET
    public List<ClienteResponse> listar() {
        // Uma linha por cliente, com a conta principal (a primeira aberta), numa consulta só.
        List<Conta> principais = Conta.list("from Conta c join fetch c.titular t where t.perfil = ?1 and c.id = "
                + "(select min(c2.id) from Conta c2 where c2.titular = t) order by t.nome", Perfil.CLIENTE);
        return principais.stream().map(c -> ClienteResponse.de(c.titular, c)).toList();
    }

    @GET
    @Path("/{id}")
    public ClienteResponse buscar(@PathParam("id") Long id) {
        Usuario usuario = clienteService.buscar(id);
        return ClienteResponse.de(usuario, Conta.principalDe(usuario));
    }

    /** Cadastra o cliente e abre a conta. A resposta traz a senha provisória (única vez que aparece). */
    @POST
    public Response cadastrar(@Valid NovoClienteRequest req) {
        ClienteService.Cadastro cadastro = clienteService.cadastrar(req);
        Conta conta = cadastro.conta();
        NovoClienteResponse corpo = new NovoClienteResponse(
                ClienteResponse.de(conta.titular, conta), cadastro.senhaProvisoria());
        return Response.status(Response.Status.CREATED).entity(corpo).build();
    }

    @PUT
    @Path("/{id}")
    public ClienteResponse editar(@PathParam("id") Long id, @Valid EditarClienteRequest req) {
        Usuario usuario = clienteService.editar(id, req);
        return ClienteResponse.de(usuario, Conta.principalDe(usuario));
    }

    /** Cliente esqueceu a senha: gera outra provisória (ele troca no próximo acesso). */
    @POST
    @Path("/{id}/senha-provisoria")
    @Consumes(MediaType.WILDCARD)
    public SenhaProvisoriaResponse novaSenha(@PathParam("id") Long id) {
        return new SenhaProvisoriaResponse(clienteService.novaSenhaProvisoria(id));
    }
}
