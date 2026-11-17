package bancohidra.controller;

import bancohidra.dto.FinancasDtos.ContaRequest;
import bancohidra.dto.FinancasDtos.MetaRequest;
import bancohidra.dto.FinancasDtos.MinhaContaResponse;
import bancohidra.dto.FinancasDtos.ProdutoResponse;
import bancohidra.dto.FinancasDtos.RenomearContaRequest;
import bancohidra.entity.Conta;
import bancohidra.entity.ContaPoupanca;
import bancohidra.entity.ProdutoInvestimento;
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

import java.time.LocalDate;
import java.util.Arrays;
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
        Conta conta = clienteService.abrirConta(eu, req.nome(), req.tipo(), req.produto());
        return Response.status(Response.Status.CREATED)
                .entity(MinhaContaResponse.de(conta, Conta.principalDe(eu).id)).build();
    }

    /** Produtos disponíveis para abrir um investimento (com a taxa ao ano e se é isento de IR). */
    @GET
    @Path("/produtos")
    public List<ProdutoResponse> produtos() {
        return Arrays.stream(ProdutoInvestimento.values()).map(ProdutoResponse::de).toList();
    }

    /** Renomeia (o tipo e o número não mudam). */
    @PUT
    @Path("/{id}")
    @Transactional
    public MinhaContaResponse renomear(@PathParam("id") Long id, @Valid RenomearContaRequest req) {
        Usuario eu = logado.usuario();
        Conta conta = contaAberta(eu, id);
        conta.nome = req.nome().trim();
        return MinhaContaResponse.de(conta, Conta.principalDe(eu).id);
    }

    /** Define (ou troca) a meta de uma poupança: quanto guardar, para quê e até quando. */
    @PUT
    @Path("/{id}/meta")
    @Transactional
    public MinhaContaResponse definirMeta(@PathParam("id") Long id, @Valid MetaRequest req) {
        Usuario eu = logado.usuario();
        poupanca(eu, id).definirMeta(req.valor(), req.descricao(), req.data(), LocalDate.now());
        return MinhaContaResponse.de(clienteService.contaDo(eu, id), Conta.principalDe(eu).id);
    }

    @DELETE
    @Path("/{id}/meta")
    @Transactional
    public MinhaContaResponse removerMeta(@PathParam("id") Long id) {
        Usuario eu = logado.usuario();
        poupanca(eu, id).removerMeta();
        return MinhaContaResponse.de(clienteService.contaDo(eu, id), Conta.principalDe(eu).id);
    }

    private Conta contaAberta(Usuario eu, Long id) {
        Conta conta = clienteService.contaDo(eu, id);
        if (conta.status == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("Conta encerrada não pode ser alterada");
        }
        return conta;
    }

    private ContaPoupanca poupanca(Usuario eu, Long id) {
        if (contaAberta(eu, id) instanceof ContaPoupanca p) {
            return p;
        }
        throw new RegraNegocioException("Só a poupança tem meta");
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
