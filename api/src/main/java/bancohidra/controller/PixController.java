package bancohidra.controller;

import bancohidra.dto.PixDtos.ChaveResponse;
import bancohidra.dto.PixDtos.DestinatarioResponse;
import bancohidra.dto.PixDtos.NovaChaveRequest;
import bancohidra.dto.PixDtos.PixRequest;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.entity.Categoria;
import bancohidra.entity.ChavePix;
import bancohidra.entity.Conta;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.service.ChavePixService;
import bancohidra.service.MovimentacaoService;
import bancohidra.util.Cpf;
import jakarta.inject.Inject;
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

import java.util.List;

@Path("/pix")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PixController {

    @Inject
    ChavePixService chavePixService;

    @Inject
    MovimentacaoService movimentacaoService;

    // ---------- chaves de uma conta ----------

    @GET
    @Path("/contas/{contaId}/chaves")
    public List<ChaveResponse> chaves(@PathParam("contaId") Long contaId) {
        Conta conta = Conta.findById(contaId);
        if (conta == null) {
            throw new NaoEncontradoException("Conta não encontrada");
        }
        return ChavePix.daConta(conta).stream().map(ChaveResponse::de).toList();
    }

    @POST
    @Path("/contas/{contaId}/chaves")
    public Response cadastrarChave(@PathParam("contaId") Long contaId, @Valid NovaChaveRequest req) {
        ChavePix chave = chavePixService.cadastrar(contaId, req.tipo(), req.valor());
        return Response.status(Response.Status.CREATED).entity(ChaveResponse.de(chave)).build();
    }

    @DELETE
    @Path("/contas/{contaId}/chaves/{chaveId}")
    public Response excluirChave(@PathParam("contaId") Long contaId, @PathParam("chaveId") Long chaveId) {
        chavePixService.excluir(contaId, chaveId);
        return Response.noContent().build();
    }

    // ---------- pagar ----------

    /** Consulta quem é o dono da chave antes de confirmar o Pix ("você está pagando..."). */
    @GET
    @Path("/destinatario")
    public DestinatarioResponse destinatario(@QueryParam("chave") String chave) {
        ChavePix encontrada = chavePixService.resolver(chave);
        Conta conta = encontrada.conta;
        return new DestinatarioResponse(conta.titular.nome, Cpf.mascarar(conta.titular.cpf), "Banco Hidra",
                conta.agencia, conta.numero, encontrada.tipo);
    }

    @POST
    @Path("/transferencias")
    public Response enviar(@Valid PixRequest req) {
        ChavePix destino = chavePixService.resolver(req.chave());
        Transacao saida = movimentacaoService.transferir(req.contaOrigemId(), destino.conta.id,
                req.valor(), req.descricao(), Categoria.PIX);
        return Response.status(Response.Status.CREATED).entity(TransacaoResponse.de(saida)).build();
    }
}
