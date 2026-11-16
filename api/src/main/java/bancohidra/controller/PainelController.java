package bancohidra.controller;

import bancohidra.dto.PainelDtos.ResumoResponse;
import bancohidra.security.Papeis;
import bancohidra.service.PainelService;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.RedeService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;

/** Números do painel inicial do administrador. */
@Path("/painel")
@RolesAllowed(Papeis.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
public class PainelController {

    @Inject
    PainelService painelService;

    @Inject
    RedeService redeService;

    @Inject
    UsuarioLogado logado;

    @ConfigProperty(name = "banco.admin.senha")
    String senhaPadraoAdmin;

    @GET
    public ResumoResponse resumo() {
        return painelService.resumo(LocalDate.now());
    }

    /**
     * "Modo turma": IPs deste PC na rede local (o painel monta o endereço para os colegas) e se o
     * administrador ainda usa a senha padrão, que está no README (com a rede aberta, é um risco).
     */
    @GET
    @Path("/rede")
    public RedeResponse rede() {
        return new RedeResponse(redeService.enderecosLocais(), redeService.tunel().orElse(null),
                logado.usuario().confereSenha(senhaPadraoAdmin));
    }

    /** {@code tunel}: link público do túnel da Cloudflare, se houver um aberto (colegas em outras redes). */
    public record RedeResponse(List<String> enderecos, String tunel, boolean senhaPadrao) {
    }
}
