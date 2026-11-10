package bancohidra.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;
import java.util.List;

/** Chave Pix ligada a uma conta. O valor é guardado já normalizado (ver ChavePixService). */
@Entity
public class ChavePix extends PanacheEntity {

    /** Limite do Banco Central para pessoa física. */
    public static final int LIMITE_POR_CONTA = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public TipoChavePix tipo;

    @Column(nullable = false, unique = true, length = 150)
    public String valor;

    @ManyToOne(optional = false)
    public Conta conta;

    @Column(nullable = false)
    public LocalDateTime criadaEm = LocalDateTime.now();

    public static ChavePix porValor(String valor) {
        return find("valor", valor).firstResult();
    }

    public static List<ChavePix> daConta(Conta conta) {
        return list("conta", conta);
    }
}
