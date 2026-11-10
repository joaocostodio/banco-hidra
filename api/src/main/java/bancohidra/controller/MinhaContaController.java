package bancohidra.controller;

import bancohidra.dto.ContaDtos.ContaResponse;
import bancohidra.dto.PixDtos.DestinatarioResponse;
import bancohidra.dto.TransacaoDtos.ComprovanteResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.dto.TransacaoDtos.TransferenciaRequest;
import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.StatusConta;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.ConsultaService;
import bancohidra.service.MovimentacaoService;
import bancohidra.util.Cpf;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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
import java.util.UUID;

/** Internet banking do cliente. A conta é sempre a de quem está logado. */
@Path("/minha-conta")
@RolesAllowed(Papeis.CLIENTE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MinhaContaController {

    @Inject
    UsuarioLogado logado;

    @Inject
    MovimentacaoService movimentacaoService;

    @Inject
    ConsultaService consultaService;

    @GET
    public ContaResponse conta() {
        return ContaResponse.de(logado.conta());
    }

    @GET
    @Path("/extrato")
    public List<TransacaoResponse> extrato(@QueryParam("inicio") LocalDate inicio,
                                           @QueryParam("fim") LocalDate fim) {
        return consultaService.extrato(logado.conta(), inicio, fim);
    }

    @GET
    @Path("/comprovantes/{autenticacao}")
    public ComprovanteResponse comprovante(@PathParam("autenticacao") UUID autenticacao) {
        return consultaService.comprovante(autenticacao, logado.conta());
    }

    /** Confere de quem é a conta antes de transferir. */
    @GET
    @Path("/destinatario")
    public DestinatarioResponse destinatario(@QueryParam("agencia") String agencia,
                                             @QueryParam("numero") String numero) {
        Conta conta = contaDestino(agencia, numero);
        return new DestinatarioResponse(conta.titular.nome, Cpf.mascarar(conta.titular.cpf), "Banco Hidra",
                conta.agencia, conta.numero, null);
    }

    @POST
    @Path("/transferencias")
    public Response transferir(@Valid TransferenciaRequest req) {
        Conta destino = contaDestino(req.agencia(), req.numero());
        Transacao saida = movimentacaoService.transferir(logado.conta().id, destino.id,
                req.valor(), req.descricao(), Categoria.TRANSFERENCIA);
        return Response.status(Response.Status.CREATED).entity(TransacaoResponse.de(saida)).build();
    }

    private static Conta contaDestino(String agencia, String numero) {
        Conta conta = numero == null ? null : Conta.porNumero(numero.trim());
        if (conta == null || !conta.agencia.equals(agencia == null ? "" : agencia.trim())
                || conta.status == StatusConta.ENCERRADA) {
            throw new NaoEncontradoException("Conta de destino não encontrada");
        }
        return conta;
    }
}
