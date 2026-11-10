package bancohidra.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.Optional;

/**
 * Classifica as transações (Pix, Depósito, Pagamento...).
 * As categorias de sistema são criadas na inicialização e não podem ser alteradas nem excluídas,
 * porque o próprio banco as usa ao registrar movimentações.
 */
@Entity
public class Categoria extends PanacheEntity {

    public static final String DEPOSITO = "Depósito";
    public static final String TRANSFERENCIA = "Transferência";
    public static final String PIX = "Pix";
    public static final String PAGAMENTO = "Pagamento";
    public static final String SAQUE = "Saque";

    @Column(nullable = false, unique = true, length = 150)
    public String descricao;

    @Column(nullable = false)
    public boolean sistema;

    public static Optional<Categoria> porDescricao(String descricao) {
        return find("lower(descricao) = lower(?1)", descricao.trim()).firstResultOptional();
    }
}
