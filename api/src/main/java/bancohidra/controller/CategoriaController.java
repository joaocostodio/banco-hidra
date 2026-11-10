package bancohidra.controller;

import bancohidra.dto.CategoriaDtos.CategoriaRequest;
import bancohidra.dto.CategoriaDtos.CategoriaResponse;
import bancohidra.entity.Categoria;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
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

@Path("/categorias")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CategoriaController {

    @GET
    public List<CategoriaResponse> listar() {
        return Categoria.<Categoria>listAll(Sort.ascending("descricao")).stream()
                .map(CategoriaResponse::de).toList();
    }

    @GET
    @Path("/{id}")
    public CategoriaResponse buscar(@PathParam("id") Long id) {
        return CategoriaResponse.de(encontrar(id));
    }

    @POST
    @Transactional
    public Response criar(@Valid CategoriaRequest req) {
        exigirDescricaoLivre(req.descricao(), null);
        Categoria categoria = new Categoria();
        categoria.descricao = req.descricao().trim();
        categoria.persist();
        return Response.status(Response.Status.CREATED).entity(CategoriaResponse.de(categoria)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public CategoriaResponse atualizar(@PathParam("id") Long id, @Valid CategoriaRequest req) {
        Categoria categoria = encontrar(id);
        if (categoria.sistema) {
            throw new RegraNegocioException("Categorias do sistema não podem ser alteradas");
        }
        exigirDescricaoLivre(req.descricao(), id);
        categoria.descricao = req.descricao().trim();
        return CategoriaResponse.de(categoria);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response excluir(@PathParam("id") Long id) {
        Categoria categoria = encontrar(id);
        if (categoria.sistema) {
            throw new RegraNegocioException("Categorias do sistema não podem ser excluídas");
        }
        if (Transacao.count("categoria", categoria) > 0) {
            throw new RegraNegocioException("Categoria em uso por transações não pode ser excluída");
        }
        categoria.delete();
        return Response.noContent().build();
    }

    private Categoria encontrar(Long id) {
        Categoria categoria = Categoria.findById(id);
        if (categoria == null) {
            throw new NaoEncontradoException("Categoria não encontrada");
        }
        return categoria;
    }

    private void exigirDescricaoLivre(String descricao, Long idAtual) {
        Categoria.porDescricao(descricao)
                .filter(existente -> !existente.id.equals(idAtual))
                .ifPresent(existente -> {
                    throw new RegraNegocioException("Já existe uma categoria com esta descrição");
                });
    }
}
