package bancohidra.controller;

import bancohidra.dto.PixDtos.ChaveResponse;
import bancohidra.dto.PixDtos.CobrancaRequest;
import bancohidra.dto.PixDtos.CobrancaResponse;
import bancohidra.dto.PixDtos.CodigoLidoResponse;
import bancohidra.dto.PixDtos.DestinatarioResponse;
import bancohidra.dto.PixDtos.LerCodigoRequest;
import bancohidra.dto.PixDtos.NovaChaveRequest;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.util.BrCode;
import bancohidra.dto.PixDtos.PixRequest;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.entity.Categoria;
import bancohidra.entity.ChavePix;
import bancohidra.entity.Conta;
import bancohidra.entity.Transacao;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.ChavePixService;
import bancohidra.service.MovimentacaoService;
import bancohidra.util.Cpf;
import jakarta.annotation.security.RolesAllowed;
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

/** Pix do cliente logado: suas chaves e envio por chave. */
@Path("/minha-conta/pix")
@RolesAllowed(Papeis.CLIENTE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PixController {

    /** Cidade que vai no QR code (campo obrigatório do padrão). */
    private static final String CIDADE = "Tres de Maio";

    @Inject
    UsuarioLogado logado;

    @Inject
    ChavePixService chavePixService;

    @Inject
    MovimentacaoService movimentacaoService;

    @GET
    @Path("/chaves")
    public List<ChaveResponse> chaves() {
        return ChavePix.daConta(logado.conta()).stream().map(ChaveResponse::de).toList();
    }

    @POST
    @Path("/chaves")
    public Response cadastrarChave(@Valid NovaChaveRequest req) {
        ChavePix chave = chavePixService.cadastrar(logado.conta().id, req.tipo(), req.valor());
        return Response.status(Response.Status.CREATED).entity(ChaveResponse.de(chave)).build();
    }

    @DELETE
    @Path("/chaves/{chaveId}")
    public Response excluirChave(@PathParam("chaveId") Long chaveId) {
        chavePixService.excluir(logado.conta().id, chaveId);
        return Response.noContent().build();
    }

    /** Quem é o dono da chave, antes de confirmar ("você está pagando..."). */
    @GET
    @Path("/destinatario")
    public DestinatarioResponse destinatario(@QueryParam("chave") String chave) {
        return destinatarioDe(chavePixService.resolver(chave));
    }

    /** Gera o "Pix copia e cola" (e o QR code) para receber numa das minhas chaves. */
    @POST
    @Path("/cobrancas")
    public CobrancaResponse cobrar(@Valid CobrancaRequest req) {
        ChavePix chave = ChavePix.findById(req.chaveId());
        Conta minha = logado.conta();
        if (chave == null || !chave.conta.id.equals(minha.id)) {
            throw new NaoEncontradoException("Chave Pix não encontrada");
        }
        String descricao = req.descricao() == null || req.descricao().isBlank() ? null : req.descricao().trim();
        String codigo = BrCode.gerar(new BrCode.Dados(chave.valor, req.valor(), descricao, minha.titular.nome,
                CIDADE, "HIDRA" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()));
        return new CobrancaResponse(codigo, chave.valor, chave.tipo,
                req.valor() == null ? null : req.valor().setScale(2), descricao);
    }

    /** Lê o código colado pelo pagador: confere se está íntegro e mostra para quem vai o dinheiro. */
    @POST
    @Path("/ler-codigo")
    public CodigoLidoResponse lerCodigo(@Valid LerCodigoRequest req) {
        BrCode.Dados dados = BrCode.ler(req.codigo());
        ChavePix chave;
        try {
            chave = chavePixService.resolver(dados.chave());
        } catch (NaoEncontradoException e) {
            throw new NaoEncontradoException("A chave deste código não é do Banco Hidra");
        }
        return new CodigoLidoResponse(dados.chave(), dados.valor(), dados.descricao(), destinatarioDe(chave));
    }

    private static DestinatarioResponse destinatarioDe(ChavePix chave) {
        Conta conta = chave.conta;
        return new DestinatarioResponse(conta.titular.nome, Cpf.mascarar(conta.titular.cpf), "Banco Hidra",
                conta.agencia, conta.numero, chave.tipo);
    }

    @POST
    public Response enviar(@Valid PixRequest req) {
        ChavePix destino = chavePixService.resolver(req.chave());
        Transacao saida = movimentacaoService.transferir(logado.conta().id, destino.conta.id,
                req.valor(), req.descricao(), Categoria.PIX);
        return Response.status(Response.Status.CREATED).entity(TransacaoResponse.de(saida)).build();
    }
}
