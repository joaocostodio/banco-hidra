package bancohidra.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Uma linha do extrato. Uma transferência gera duas transações (saída na origem e entrada
 * no destino) que compartilham o mesmo {@link #autenticacao}, usado no comprovante.
 */
@Entity
public class Transacao extends PanacheEntity {

    @ManyToOne(optional = false)
    public Conta conta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public TipoTransacao tipo;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal valor;

    /** Saldo da conta logo após esta transação (facilita o extrato). */
    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal saldoApos;

    @Column(nullable = false, length = 500)
    public String descricao;

    @ManyToOne(optional = false)
    public Categoria categoria;

    /** A outra conta envolvida (transferência/Pix); nulo em depósito. */
    @ManyToOne
    public Conta contraparte;

    @Column(nullable = false)
    public UUID autenticacao;

    @Column(nullable = false)
    public LocalDateTime dataHora = LocalDateTime.now();

    public static List<Transacao> extrato(Conta conta, LocalDateTime inicio, LocalDateTime fim) {
        return list("conta = ?1 and dataHora >= ?2 and dataHora < ?3",
                Sort.descending("dataHora", "id"), conta, inicio, fim);
    }

    public static List<Transacao> porAutenticacao(UUID autenticacao) {
        return list("autenticacao", Sort.ascending("id"), autenticacao);
    }

    public static List<Transacao> ultimas(int quantidade) {
        return findAll(Sort.descending("dataHora", "id")).page(0, quantidade).list();
    }
}
