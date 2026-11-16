package bancohidra.controller;

import bancohidra.dto.FinancasDtos.CategoriaClienteResponse;
import bancohidra.dto.FinancasDtos.CategoriaPropriaRequest;
import bancohidra.entity.Categoria;
import bancohidra.entity.Usuario;
import bancohidra.security.Papeis;
import bancohidra.security.UsuarioLogado;
import bancohidra.service.FinancasService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Categorias que o cliente enxerga: as padrão do banco, as de sistema (para filtrar o extrato) e as
 * que ele mesmo criou. Só as próprias podem ser alteradas ou excluídas.
 */
@Path("/minha-conta/categorias")
@RolesAllowed(Papeis.CLIENTE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MinhasCategoriasController {

    @Inject
    UsuarioLogado logado;

    @Inject
    FinancasService financasService;

    @GET
    public List<CategoriaClienteResponse> listar() {
        Usuario eu = logado.usuario();
        return Categoria.visiveisPara(eu).stream().map(c -> CategoriaClienteResponse.de(c, eu)).toList();
    }

    @POST
    public Response criar(@Valid CategoriaPropriaRequest req) {
        Usuario eu = logado.usuario();
        Categoria categoria = financasService.criarCategoria(eu, req.descricao());
        return Response.status(Response.Status.CREATED).entity(CategoriaClienteResponse.de(categoria, eu)).build();
    }

    @PUT
    @Path("/{id}")
    public CategoriaClienteResponse renomear(@PathParam("id") Long id, @Valid CategoriaPropriaRequest req) {
        Usuario eu = logado.usuario();
        return CategoriaClienteResponse.de(financasService.renomearCategoria(eu, id, req.descricao()), eu);
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        financasService.excluirCategoria(logado.usuario(), id);
        return Response.noContent().build();
    }
}
