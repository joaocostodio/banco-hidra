package bancohidra.controller;

import bancohidra.dto.BoletoDtos.BoletoResponse;
import bancohidra.dto.BoletoDtos.ConsultaBoletoResponse;
import bancohidra.dto.BoletoDtos.EmitirBoletoRequest;
import bancohidra.dto.BoletoDtos.PagarBoletoRequest;
import bancohidra.dto.PixDtos.DestinatarioResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.entity.Boleto;
import bancohidra.entity.Conta;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.BoletoService;
import bancohidra.util.CodigoBoleto;
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

import java.time.LocalDate;
import java.util.List;

/** Boletos do cliente logado: os que ele emite para receber e o pagamento dos de outros. */
@Path("/minha-conta/boletos")
@RolesAllowed(Papeis.CLIENTE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BoletoController {

    @Inject
    UsuarioLogado logado;

    @Inject
    BoletoService boletoService;

    @GET
    public List<BoletoResponse> emitidos() {
        LocalDate hoje = LocalDate.now();
        return Boleto.emitidosPor(logado.conta()).stream().map(b -> BoletoResponse.de(b, hoje)).toList();
    }

    @GET
    @Path("/{id}")
    public BoletoResponse buscar(@PathParam("id") Long id) {
        Boleto boleto = Boleto.findById(id);
        if (boleto == null || !boleto.beneficiario.id.equals(logado.conta().id)) {
            throw new NaoEncontradoException("Boleto não encontrado");
        }
        return BoletoResponse.de(boleto, LocalDate.now());
    }

    @POST
    public Response emitir(@Valid EmitirBoletoRequest req) {
        Boleto boleto = boletoService.emitir(logado.conta(), req.valor(), req.vencimento(), req.descricao());
        return Response.status(Response.Status.CREATED).entity(BoletoResponse.de(boleto, LocalDate.now())).build();
    }

    @DELETE
    @Path("/{id}")
    public BoletoResponse cancelar(@PathParam("id") Long id) {
        return BoletoResponse.de(boletoService.cancelar(logado.conta(), id), LocalDate.now());
    }

    /** "Confira antes de pagar": beneficiário, vencimento e o total de hoje (com multa e juros). */
    @GET
    @Path("/consulta")
    public ConsultaBoletoResponse consultar(@QueryParam("linha") String linha) {
        Boleto boleto = boletoService.consultar(linha);
        Boleto.Encargos e = boleto.encargosEm(LocalDate.now());
        Conta c = boleto.beneficiario;
        DestinatarioResponse beneficiario = new DestinatarioResponse(c.titular.nome, Cpf.mascarar(c.titular.cpf),
                "Banco Hidra", c.agencia, c.numero, null);
        return new ConsultaBoletoResponse(CodigoBoleto.formatar(boleto.linhaDigitavel), beneficiario,
                boleto.descricao, boleto.vencimento, boleto.valor, e.diasAtraso(), e.multa(), e.juros(), e.total(),
                boleto.getStatus());
    }

    @POST
    @Path("/pagamento")
    public Response pagar(@Valid PagarBoletoRequest req) {
        Transacao saida = boletoService.pagar(logado.conta(), req.linha());
        return Response.status(Response.Status.CREATED).entity(TransacaoResponse.de(saida)).build();
    }
}
