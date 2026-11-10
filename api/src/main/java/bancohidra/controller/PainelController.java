package bancohidra.controller;

import bancohidra.dto.PainelDtos.ResumoResponse;
import bancohidra.security.Papeis;
import bancohidra.service.PainelService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;

/** Números do painel inicial do administrador. */
@Path("/painel")
@RolesAllowed(Papeis.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
public class PainelController {

    @Inject
    PainelService painelService;

    @GET
    public ResumoResponse resumo() {
        return painelService.resumo(LocalDate.now());
    }
}
