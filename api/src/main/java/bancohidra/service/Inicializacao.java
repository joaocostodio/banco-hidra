package bancohidra.service;

import bancohidra.entity.Categoria;
import bancohidra.entity.Perfil;
import bancohidra.entity.Usuario;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

/**
 * Ao subir a API garante o mínimo para o banco funcionar: as categorias usadas pelo
 * próprio sistema e um administrador (configurado em application.properties).
 */
@ApplicationScoped
public class Inicializacao {

    private static final List<String> CATEGORIAS_SISTEMA = List.of(
            Categoria.DEPOSITO, Categoria.TRANSFERENCIA, Categoria.PIX, Categoria.PAGAMENTO, Categoria.SAQUE);

    @ConfigProperty(name = "banco.admin.nome")
    String adminNome;

    @ConfigProperty(name = "banco.admin.email")
    String adminEmail;

    @ConfigProperty(name = "banco.admin.senha")
    String adminSenha;

    @Transactional
    void aoIniciar(@Observes StartupEvent evento) {
        for (String descricao : CATEGORIAS_SISTEMA) {
            Categoria categoria = Categoria.porDescricao(descricao).orElseGet(Categoria::new);
            categoria.descricao = descricao;
            categoria.sistema = true;
            categoria.persist();
        }

        if (Usuario.count("perfil", Perfil.ADMIN) == 0) {
            Usuario admin = new Usuario();
            admin.nome = adminNome;
            admin.email = adminEmail.toLowerCase();
            admin.perfil = Perfil.ADMIN;
            admin.definirSenha(adminSenha);
            admin.persist();
            Log.infof("Administrador criado: %s", admin.email);
        }
    }
}
