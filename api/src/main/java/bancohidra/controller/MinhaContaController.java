package bancohidra.controller;

import bancohidra.dto.ContaDtos.ContaResponse;
import bancohidra.dto.FinancasDtos.LancamentoRequest;
import bancohidra.dto.FinancasDtos.ResumoResponse;
import bancohidra.dto.PixDtos.DestinatarioResponse;
import bancohidra.dto.TransacaoDtos.ComprovanteResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.dto.TransacaoDtos.TransferenciaRequest;
import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.StatusConta;
import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Transacao;
import bancohidra.entity.Usuario;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.ClienteService;
import bancohidra.service.ConsultaService;
import bancohidra.service.FinancasService;
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

/**
 * Internet banking e gestão financeira do cliente. Tudo parte do usuário do token: as contas,
 * categorias e transações de outros clientes nunca aparecem (respondem "não encontrado").
 */
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

    @Inject
    FinancasService financasService;

    @Inject
    ClienteService clienteService;

    /** A conta principal (a das chaves Pix e dos boletos). */
    @GET
    public ContaResponse conta() {
        return ContaResponse.de(logado.conta());
    }

    /** Dados do titular para a tela de perfil (o CPF aparece completo só para o próprio dono). */
    @GET
    @Path("/dados")
    public MeusDadosResponse dados() {
        Conta conta = logado.conta();
        Usuario u = conta.titular;
        return new MeusDadosResponse(u.nome, u.email, u.cpf == null ? null : Cpf.formatar(u.cpf),
                conta.agencia, conta.numero, conta.status, u.criadoEm);
    }

    public record MeusDadosResponse(String nome, String email, String cpf, String agencia, String numero,
                                    StatusConta status, java.time.LocalDateTime clienteDesde) {
    }

    /** Tela inicial: saldo por conta e geral, receitas e despesas do mês. */
    @GET
    @Path("/resumo")
    public ResumoResponse resumo() {
        return financasService.resumo(logado.usuario(), LocalDate.now());
    }

    /**
     * Transações de todas as minhas contas, com filtros opcionais por conta, categoria, tipo
     * (ENTRADA/SAIDA) e período (datas AAAA-MM-DD; padrão: últimos 30 dias).
     */
    @GET
    @Path("/transacoes")
    public List<TransacaoResponse> transacoes(@QueryParam("contaId") Long contaId,
                                              @QueryParam("categoriaId") Long categoriaId,
                                              @QueryParam("tipo") TipoTransacao tipo,
                                              @QueryParam("inicio") LocalDate inicio,
                                              @QueryParam("fim") LocalDate fim) {
        return financasService.transacoes(logado.usuario(), contaId, categoriaId, tipo, inicio, fim)
                .stream().map(TransacaoResponse::de).toList();
    }

    /** Lançar uma receita ou despesa numa das minhas contas. */
    @POST
    @Path("/lancamentos")
    public Response lancar(@Valid LancamentoRequest req) {
        Transacao t = financasService.lancar(logado.usuario(), req);
        return Response.status(Response.Status.CREATED).entity(TransacaoResponse.de(t)).build();
    }

    @GET
    @Path("/comprovantes/{autenticacao}")
    public ComprovanteResponse comprovante(@PathParam("autenticacao") UUID autenticacao) {
        return consultaService.comprovante(autenticacao, logado.usuario());
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

    /** Transferência para outra pessoa ou entre as minhas contas (a origem é sempre uma conta minha). */
    @POST
    @Path("/transferencias")
    public Response transferir(@Valid TransferenciaRequest req) {
        Usuario eu = logado.usuario();
        Conta origem = req.contaOrigemId() == null ? logado.conta() : clienteService.contaDo(eu, req.contaOrigemId());
        Conta destino = contaDestino(req.agencia(), req.numero());
        Transacao saida = movimentacaoService.transferir(origem.id, destino.id, req.valor(), req.descricao(),
                Categoria.TRANSFERENCIA);
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
