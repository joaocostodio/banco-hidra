package bancohidra.controller;

import bancohidra.dto.FinancasDtos.ContaRequest;
import bancohidra.dto.FinancasDtos.MinhaContaResponse;
import bancohidra.entity.Conta;
import bancohidra.entity.StatusConta;
import bancohidra.entity.Usuario;
import bancohidra.exception.RegraNegocioException;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.ClienteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/** As contas do cliente logado: abrir (corrente, poupança, carteira, investimento), renomear e encerrar. */
@Path("/minha-conta/contas")
@RolesAllowed(Papeis.CLIENTE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MinhasContasController {

    @Inject
    UsuarioLogado logado;

    @Inject
    ClienteService clienteService;

    /** Todas as minhas contas, a principal primeiro (as encerradas também, para o histórico). */
    @GET
    public List<MinhaContaResponse> listar() {
        Usuario eu = logado.usuario();
        Long principal = Conta.principalDe(eu).id;
        return Conta.doTitular(eu).stream().map(c -> MinhaContaResponse.de(c, principal)).toList();
    }

    @POST
    public Response abrir(@Valid ContaRequest req) {
        Usuario eu = logado.usuario();
        Conta conta = clienteService.abrirConta(eu, req.nome(), req.tipo());
        return Response.status(Response.Status.CREATED)
                .entity(MinhaContaResponse.de(conta, Conta.principalDe(eu).id)).build();
    }

    /** Muda o nome ou o tipo (o número da conta não muda). */
    @PUT
    @Path("/{id}")
    @Transactional
    public MinhaContaResponse editar(@PathParam("id") Long id, @Valid ContaRequest req) {
        Usuario eu = logado.usuario();
        Conta conta = clienteService.contaDo(eu, id);
        if (conta.status == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("Conta encerrada não pode ser alterada");
        }
        conta.nome = req.nome().trim();
        conta.tipo = req.tipo();
        return MinhaContaResponse.de(conta, Conta.principalDe(eu).id);
    }

    /** Encerra (só com saldo zero, e nunca a principal). O histórico continua no extrato. */
    @DELETE
    @Path("/{id}")
    public MinhaContaResponse encerrar(@PathParam("id") Long id) {
        Usuario eu = logado.usuario();
        Conta conta = clienteService.encerrarConta(eu, id);
        return MinhaContaResponse.de(conta, Conta.principalDe(eu).id);
    }
}
