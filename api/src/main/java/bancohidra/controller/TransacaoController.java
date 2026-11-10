package bancohidra.controller;

import bancohidra.dto.TransacaoDtos.ComprovanteResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.security.Papeis;
import bancohidra.service.ConsultaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

/**
 * Movimentações do banco inteiro, para o administrador acompanhar.
 * O administrador não movimenta dinheiro de clientes (só deposita, em /contas/{id}/depositos);
 * transferências e Pix são feitos pelo próprio cliente em /minha-conta.
 */
@Path("/transacoes")
@RolesAllowed(Papeis.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
public class TransacaoController {

    @Inject
    ConsultaService consultaService;

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
        return consultaService.comprovante(autenticacao, null);
    }
}
