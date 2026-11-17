package bancohidra.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.util.List;
import java.util.Optional;

/**
 * Classifica as transações. Há três tipos:
 * <ul>
 *   <li><b>de sistema</b> ({@code sistema = true}): Pix, Depósito, Transferência... O próprio banco as usa ao
 *       registrar movimentações; ninguém altera nem exclui.</li>
 *   <li><b>padrão</b> ({@code dono = null}): Alimentação, Moradia... Valem para todos os clientes e só o
 *       administrador gerencia.</li>
 *   <li><b>próprias</b> ({@code dono} preenchido): criadas pelo cliente; só ele vê e usa.</li>
 * </ul>
 * O nome é único entre as que um mesmo cliente enxerga (as padrão e as dele), sem diferenciar maiúsculas.
 */
@Entity
public class Categoria extends PanacheEntity {

    public static final String DEPOSITO = "Depósito";
    public static final String TRANSFERENCIA = "Transferência";
    public static final String PIX = "Pix";
    public static final String PAGAMENTO = "Pagamento";
    public static final String SAQUE = "Saque";
    public static final String RENDIMENTO = "Rendimento";
    public static final String IMPOSTOS = "Impostos";

    @Column(nullable = false, length = 150)
    public String descricao;

    @Column(nullable = false)
    public boolean sistema;

    /** Cliente dono da categoria; nulo nas categorias de sistema e nas padrão. */
    @ManyToOne
    public Usuario dono;

    public boolean isPropriaDe(Usuario usuario) {
        return dono != null && dono.id.equals(usuario.id);
    }

    /** Pode ser usada por este cliente num lançamento (as de sistema ficam de fora: são do banco). */
    public boolean podeSerUsadaPor(Usuario usuario) {
        return !sistema && (dono == null || isPropriaDe(usuario));
    }

    // ---------- consultas ----------

    /** Categoria de sistema ou padrão (sem dono) com esta descrição. */
    public static Optional<Categoria> globalPorDescricao(String descricao) {
        return find("dono is null and lower(descricao) = lower(?1)", descricao.trim()).firstResultOptional();
    }

    /** Alguma categoria que o cliente enxerga (padrão ou dele) já tem esta descrição? */
    public static boolean descricaoEmUso(String descricao, Usuario usuario, Long ignorarId) {
        return count("lower(descricao) = lower(?1) and (dono is null or dono = ?2) and id <> ?3",
                descricao.trim(), usuario, ignorarId == null ? -1L : ignorarId) > 0;
    }

    /** Já existe categoria global (sistema ou padrão) com esta descrição? Usado pelo administrador. */
    public static boolean descricaoGlobalEmUso(String descricao, Long ignorarId) {
        return count("dono is null and lower(descricao) = lower(?1) and id <> ?2",
                descricao.trim(), ignorarId == null ? -1L : ignorarId) > 0;
    }

    /** Categorias globais (sistema e padrão), para o administrador. */
    public static List<Categoria> globais() {
        return list("dono is null", Sort.ascending("descricao"));
    }

    /** O que o cliente vê: as padrão, as de sistema e as dele (nunca as de outros clientes). */
    public static List<Categoria> visiveisPara(Usuario usuario) {
        return list("dono is null or dono = ?1", Sort.ascending("descricao"), usuario);
    }
}
