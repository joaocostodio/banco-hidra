package bancohidra.controller;

import bancohidra.dto.TransacaoDtos.ComprovanteResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.dto.TransacaoDtos.TransferenciaRequest;
import bancohidra.entity.Categoria;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.service.MovimentacaoService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/transacoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TransacaoController {

    @Inject
    MovimentacaoService movimentacaoService;

    /** Últimas movimentações do banco inteiro (painel do administrador). */
    @GET
    public List<TransacaoResponse> ultimas(@QueryParam("limite") @DefaultValue("50") int limite) {
        return Transacao.ultimas(Math.clamp(limite, 1, 200)).stream().map(TransacaoResponse::de).toList();
    }

    @GET
    @Path("/{id}")
    public TransacaoResponse buscar(@PathParam("id") Long id) {
        Transacao t = Transacao.findById(id);
        if (t == null) {
            throw new NaoEncontradoException("Transação não encontrada");
        }
        return TransacaoResponse.de(t);
    }

    @GET
    @Path("/comprovantes/{autenticacao}")
    public ComprovanteResponse comprovante(@PathParam("autenticacao") UUID autenticacao) {
        List<Transacao> pernas = Transacao.porAutenticacao(autenticacao);
        if (pernas.isEmpty()) {
            throw new NaoEncontradoException("Comprovante não encontrado");
        }
        return ComprovanteResponse.de(pernas);
    }

    /** Transferência entre contas do Banco Hidra (pelo id da conta). */
    @POST
    @Path("/transferencias")
    public Response transferir(@Valid TransferenciaRequest req) {
        Transacao saida = movimentacaoService.transferir(req.contaOrigemId(), req.contaDestinoId(),
                req.valor(), req.descricao(), Categoria.TRANSFERENCIA);
        return Response.status(Response.Status.CREATED).entity(TransacaoResponse.de(saida)).build();
    }
}
