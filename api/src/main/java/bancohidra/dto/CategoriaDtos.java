package bancohidra.dto;

import bancohidra.entity.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoriaDtos {

    private CategoriaDtos() {
    }

    public record CategoriaRequest(
            @NotBlank(message = "Informe a descrição") @Size(max = 150) String descricao) {
    }

    /** {@code usos}: quantas linhas de extrato usam a categoria (só pode excluir com zero). */
    public record CategoriaResponse(Long id, String descricao, boolean sistema, long usos) {
        public static CategoriaResponse de(Categoria c, long usos) {
            return new CategoriaResponse(c.id, c.descricao, c.sistema, usos);
        }
    }
}
