package bancohidra.service;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Ajustes no banco que o Hibernate (schema-management=update) não faz sozinho: ele cria tabelas, colunas e
 * restrições novas, mas nunca apaga as antigas. Roda antes da {@link Inicializacao} e não faz nada se o banco
 * já estiver certo.
 */
@ApplicationScoped
public class MigracaoBanco {

    @Inject
    EntityManager em;

    @Transactional
    void aoIniciar(@Observes @Priority(1) StartupEvent evento) {
        removerUnicidadeGlobalDaCategoria();
    }

    /**
     * Antes das categorias próprias, a descrição da categoria era única no banco inteiro. Agora dois clientes
     * podem ter, cada um, a sua "Academia" (a unicidade por cliente é conferida em FinancasService).
     */
    @SuppressWarnings("unchecked")
    private void removerUnicidadeGlobalDaCategoria() {
        List<String> restricoes = em.createNativeQuery("""
                select con.conname from pg_constraint con
                join pg_class tab on tab.oid = con.conrelid
                join pg_attribute col on col.attrelid = tab.oid and col.attnum = con.conkey[1]
                where tab.relname = 'categoria' and con.contype = 'u'
                  and array_length(con.conkey, 1) = 1 and col.attname = 'descricao'
                """).getResultList();
        for (String nome : restricoes) {
            // O nome vem do próprio catálogo do Postgres (não do usuário); as aspas protegem mesmo assim.
            em.createNativeQuery("alter table categoria drop constraint \"" + nome.replace("\"", "") + "\"")
                    .executeUpdate();
            Log.infof("Migração: removida a restrição %s (descrição de categoria única no banco inteiro)", nome);
        }
    }
}
