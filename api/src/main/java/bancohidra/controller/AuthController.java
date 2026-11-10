package bancohidra.controller;

import bancohidra.dto.AuthDtos.LoginRequest;
import bancohidra.dto.AuthDtos.LoginResponse;
import bancohidra.dto.AuthDtos.TrocarSenhaRequest;
import bancohidra.dto.AuthDtos.UsuarioResponse;
import bancohidra.exception.NaoAutorizadoException;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthService authService;

    @Inject
    UsuarioLogado logado;

    @POST
    @Path("/login")
    @PermitAll
    public LoginResponse login(@Valid LoginRequest req) {
        // A transação de verificarCredenciais já terminou aqui, então a contagem de
        // tentativas erradas fica gravada mesmo quando respondemos 401.
        AuthService.Resultado resultado = authService.verificarCredenciais(req.email(), req.senha());
        if (resultado.erro() != null) {
            throw new NaoAutorizadoException(resultado.erro());
        }
        return resultado.resposta();
    }

    @GET
    @Path("/eu")
    @RolesAllowed({Papeis.ADMIN, Papeis.CLIENTE, Papeis.TROCAR_SENHA})
    public UsuarioResponse eu() {
        return UsuarioResponse.de(logado.usuario());
    }

    /** Troca a senha e devolve um token novo (obrigatório no primeiro acesso). */
    @PUT
    @Path("/senha")
    @RolesAllowed({Papeis.ADMIN, Papeis.CLIENTE, Papeis.TROCAR_SENHA})
    public LoginResponse trocarSenha(@Valid TrocarSenhaRequest req) {
        return authService.trocarSenha(logado.id(), req.senhaAtual(), req.novaSenha());
    }
}
