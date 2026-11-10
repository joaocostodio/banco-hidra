package bancohidra.service;

import bancohidra.entity.Categoria;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.util.List;

/** Garante, ao subir a API, que as categorias usadas pelo próprio banco existem. */
@ApplicationScoped
public class Inicializacao {

    private static final List<String> CATEGORIAS_SISTEMA = List.of(
            Categoria.DEPOSITO, Categoria.TRANSFERENCIA, Categoria.PIX, Categoria.PAGAMENTO, Categoria.SAQUE);

    @Transactional
    void aoIniciar(@Observes StartupEvent evento) {
        for (String descricao : CATEGORIAS_SISTEMA) {
            Categoria categoria = Categoria.porDescricao(descricao).orElseGet(Categoria::new);
            categoria.descricao = descricao;
            categoria.sistema = true;
            categoria.persist();
        }
    }
}
