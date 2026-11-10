package bancohidra.controller;

import bancohidra.dto.ContaDtos.ContaResponse;
import bancohidra.dto.ContaDtos.DepositoRequest;
import bancohidra.dto.PixDtos.ChaveResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.entity.ChavePix;
import bancohidra.entity.Conta;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.security.Papeis;
import bancohidra.service.ConsultaService;
import bancohidra.service.MovimentacaoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;

/** Gestão das contas pelo administrador. O cliente usa /minha-conta. */
@Path("/contas")
@RolesAllowed(Papeis.ADMIN)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ContaController {

    @Inject
    MovimentacaoService movimentacaoService;

    @Inject
    ConsultaService consultaService;

    @GET
    public List<ContaResponse> listar() {
        List<Conta> contas = Conta.list("from Conta c join fetch c.titular t order by t.nome");
        return contas.stream().map(ContaResponse::de).toList();
    }

    @GET
    @Path("/{id}")
    public ContaResponse buscar(@PathParam("id") Long id) {
        return ContaResponse.de(encontrar(id));
    }

    /** Datas no formato AAAA-MM-DD (padrão: últimos 30 dias). */
    @GET
    @Path("/{id}/extrato")
    public List<TransacaoResponse> extrato(@PathParam("id") Long id,
                                           @QueryParam("inicio") LocalDate inicio,
                                           @QueryParam("fim") LocalDate fim) {
        return consultaService.extrato(encontrar(id), inicio, fim);
    }

    @GET
    @Path("/{id}/chaves-pix")
    public List<ChaveResponse> chavesPix(@PathParam("id") Long id) {
        return ChavePix.daConta(encontrar(id)).stream().map(ChaveResponse::de).toList();
    }

    /** Depósito feito pelo banco (ex.: crédito inicial dado pelo administrador). */
    @POST
    @Path("/{id}/depositos")
    public Response depositar(@PathParam("id") Long id, @Valid DepositoRequest req) {
        Transacao t = movimentacaoService.depositar(id, req.valor(), req.descricao());
        return Response.status(Response.Status.CREATED).entity(TransacaoResponse.de(t)).build();
    }

    @POST
    @Path("/{id}/bloqueio")
    @Consumes(MediaType.WILDCARD) // não tem corpo
    @Transactional
    public ContaResponse bloquear(@PathParam("id") Long id) {
        Conta conta = encontrar(id);
        conta.bloquear();
        return ContaResponse.de(conta);
    }

    @DELETE
    @Path("/{id}/bloqueio")
    @Transactional
    public ContaResponse desbloquear(@PathParam("id") Long id) {
        Conta conta = encontrar(id);
        conta.desbloquear();
        return ContaResponse.de(conta);
    }

    /** Conta de banco não se apaga (o histórico precisa ficar): ela é encerrada. */
    @DELETE
    @Path("/{id}")
    @Transactional
    public ContaResponse encerrar(@PathParam("id") Long id) {
        Conta conta = encontrar(id);
        conta.encerrar();
        return ContaResponse.de(conta);
    }

    private Conta encontrar(Long id) {
        Conta conta = Conta.findById(id);
        if (conta == null) {
            throw new NaoEncontradoException("Conta não encontrada");
        }
        return conta;
    }
}
