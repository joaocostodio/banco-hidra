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
 * Ao subir a API garante o mínimo para o banco funcionar: as categorias usadas pelo próprio
 * sistema, as categorias padrão dos lançamentos e um administrador (configurado em application.properties).
 */
@ApplicationScoped
public class Inicializacao {

    private static final List<String> CATEGORIAS_SISTEMA = List.of(
            Categoria.DEPOSITO, Categoria.TRANSFERENCIA, Categoria.PIX, Categoria.PAGAMENTO, Categoria.SAQUE);

    /** Categorias padrão para os lançamentos dos clientes (o administrador pode editar e criar outras). */
    private static final List<String> CATEGORIAS_PADRAO = List.of(
            "Alimentação", "Moradia", "Transporte", "Saúde", "Educação", "Lazer", "Compras", "Salário",
            "Investimentos", "Outros");

    @ConfigProperty(name = "banco.admin.nome")
    String adminNome;

    @ConfigProperty(name = "banco.admin.email")
    String adminEmail;

    @ConfigProperty(name = "banco.admin.senha")
    String adminSenha;

    @Transactional
    void aoIniciar(@Observes StartupEvent evento) {
        for (String descricao : CATEGORIAS_SISTEMA) {
            Categoria categoria = Categoria.globalPorDescricao(descricao).orElseGet(Categoria::new);
            categoria.descricao = descricao;
            categoria.sistema = true;
            categoria.persist();
        }
        // As padrão só são criadas se faltarem: o administrador pode ter renomeado ou excluído alguma.
        if (Categoria.count("dono is null and sistema = false") == 0) {
            for (String descricao : CATEGORIAS_PADRAO) {
                Categoria categoria = new Categoria();
                categoria.descricao = descricao;
                categoria.persist();
            }
        }

        // Só cria: se o administrador já existe, a senha que ele trocou pelo painel é mantida.
        if (Usuario.porEmail(adminEmail) == null) {
            Usuario admin = new Usuario();
            admin.nome = adminNome;
            admin.email = adminEmail.trim().toLowerCase();
            admin.perfil = Perfil.ADMIN;
            admin.definirSenhaDoServidor(adminSenha);
            admin.persist();
            Log.infof("Administrador criado: %s", admin.email);
        }
    }
}
