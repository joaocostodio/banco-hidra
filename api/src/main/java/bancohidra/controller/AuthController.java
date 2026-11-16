package bancohidra.controller;

import bancohidra.dto.AuthDtos.CadastroRequest;
import bancohidra.dto.AuthDtos.LoginRequest;
import bancohidra.dto.AuthDtos.TrocarSenhaRequest;
import bancohidra.dto.AuthDtos.UsuarioResponse;
import bancohidra.entity.Usuario;
import bancohidra.exception.NaoAutorizadoException;
import bancohidra.security.CookieSessao;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.AuthService;
import bancohidra.service.ClienteService;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Entrar, sair, criar conta e trocar senha. O token JWT vai sempre num cookie httpOnly
 * (ver {@link CookieSessao}); o corpo da resposta só traz o que o site precisa mostrar.
 */
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthService authService;

    @Inject
    ClienteService clienteService;

    @Inject
    CookieSessao cookieSessao;

    @Inject
    UsuarioLogado logado;

    @Context
    HttpHeaders cabecalhos;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest req) {
        // A transação de verificarCredenciais já terminou aqui, então a contagem de
        // tentativas erradas fica gravada mesmo quando respondemos 401.
        AuthService.Resultado resultado = authService.verificarCredenciais(req.email(), req.senha());
        if (resultado.erro() != null) {
            throw new NaoAutorizadoException(resultado.erro());
        }
        return comSessao(Response.ok(), resultado.sessao());
    }

    /** "Criar conta": a pessoa se cadastra, ganha a conta corrente e já entra logada. */
    @POST
    @Path("/cadastro")
    @PermitAll
    public Response cadastrar(@Valid CadastroRequest req) {
        Usuario usuario = clienteService.cadastrarProprio(req);
        return comSessao(Response.status(Response.Status.CREATED), authService.emitir(usuario));
    }

    /** Sair: o navegador apaga o cookie. Funciona mesmo com a sessão já vencida. */
    @POST
    @Path("/sair")
    @PermitAll
    @Consumes(MediaType.WILDCARD) // não tem corpo
    public Response sair() {
        return Response.noContent().cookie(cookieSessao.apagar(cabecalhos)).build();
    }

    @GET
    @Path("/eu")
    @RolesAllowed({Papeis.ADMIN, Papeis.CLIENTE, Papeis.TROCAR_SENHA})
    public UsuarioResponse eu() {
        return UsuarioResponse.de(logado.usuario());
    }

    /** Troca a senha e abre uma sessão nova (obrigatório no primeiro acesso). */
    @PUT
    @Path("/senha")
    @RolesAllowed({Papeis.ADMIN, Papeis.CLIENTE, Papeis.TROCAR_SENHA})
    public Response trocarSenha(@Valid TrocarSenhaRequest req) {
        return comSessao(Response.ok(), authService.trocarSenha(logado.id(), req.senhaAtual(), req.novaSenha()));
    }

    private Response comSessao(Response.ResponseBuilder resposta, AuthService.Sessao sessao) {
        return resposta.cookie(cookieSessao.criar(sessao.token(), cabecalhos)).entity(sessao.resposta()).build();
    }
}
