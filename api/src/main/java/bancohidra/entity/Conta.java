package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import bancohidra.exception.SaldoInsuficienteException;
import bancohidra.util.Dinheiro;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Conta corrente do Banco Hidra.
 *
 * <p>O saldo é <b>encapsulado</b>: o campo é privado, não tem setter e só muda pelos
 * métodos {@link #depositar} e {@link #sacar}, que aplicam as regras do banco
 * (valor positivo, conta ativa, saldo suficiente). É a {@code ContaBancaria}
 * da aula de encapsulamento, agora persistida no banco de dados.
 */
@Entity
public class Conta extends PanacheEntity {

    public static final String AGENCIA_PADRAO = "0001";
    public static final String NOME_PRINCIPAL = "Conta corrente";
    /** Quantas contas abertas (não encerradas) um cliente pode ter. */
    public static final int LIMITE_POR_CLIENTE = 10;

    /**
     * Nome que o cliente dá à conta ("Carteira", "Reserva de emergência"...).
     * O default na coluna preenche as contas que já existiam antes deste campo.
     */
    @Column(nullable = false, length = 40, columnDefinition = "varchar(40) default '" + NOME_PRINCIPAL + "' not null")
    public String nome = NOME_PRINCIPAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, columnDefinition = "varchar(15) default 'CORRENTE' not null")
    public TipoConta tipo = TipoConta.CORRENTE;

    @Column(nullable = false, length = 4)
    public String agencia = AGENCIA_PADRAO;

    /** Número com dígito verificador, ex.: "000042-5". Sorteado na abertura da conta. */
    @Column(nullable = false, unique = true, length = 8)
    public String numero;

    @ManyToOne(optional = false)
    public Usuario titular;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public StatusConta status = StatusConta.ATIVA;

    @Column(nullable = false)
    public LocalDateTime abertaEm = LocalDateTime.now();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO.setScale(2);

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void depositar(BigDecimal valor) {
        BigDecimal v = Dinheiro.exigirPositivo(valor);
        exigirAtiva();
        saldo = saldo.add(v);
    }

    public void sacar(BigDecimal valor) {
        BigDecimal v = Dinheiro.exigirPositivo(valor);
        exigirAtiva();
        if (saldo.compareTo(v) < 0) {
            throw new SaldoInsuficienteException();
        }
        saldo = saldo.subtract(v);
    }

    public void bloquear() {
        if (status == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("Conta encerrada não pode ser bloqueada");
        }
        status = StatusConta.BLOQUEADA;
    }

    public void desbloquear() {
        if (status == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("Conta encerrada não pode ser reaberta");
        }
        status = StatusConta.ATIVA;
    }

    public void encerrar() {
        if (saldo.signum() != 0) {
            throw new RegraNegocioException("Só é possível encerrar uma conta com saldo zero");
        }
        status = StatusConta.ENCERRADA;
    }

    public boolean isAtiva() {
        return status == StatusConta.ATIVA;
    }

    private void exigirAtiva() {
        if (!isAtiva()) {
            throw new RegraNegocioException("A conta " + numero + " está " + status.name().toLowerCase());
        }
    }

    /**
     * Monta o número da conta: 6 dígitos + dígito verificador (módulo 11).
     * Ex.: 42 → "000042-5".
     */
    public static String gerarNumero(long sequencial) {
        String base = String.format("%06d", sequencial);
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * (7 - i);
        }
        int dv = soma % 11;
        return base + "-" + (dv >= 10 ? 0 : dv);
    }

    public static Conta porNumero(String numero) {
        return find("numero", numero).firstResult();
    }

    /**
     * A conta principal do cliente: a primeira que ele abriu (é a das chaves Pix e dos boletos).
     * Ela não pode ser encerrada pelo cliente.
     */
    public static Conta principalDe(Usuario titular) {
        return find("titular = ?1 order by id", titular).firstResult();
    }

    /** Todas as contas do cliente, a principal primeiro (inclui as encerradas, para o histórico). */
    public static List<Conta> doTitular(Usuario titular) {
        return list("titular = ?1 order by id", titular);
    }

    public static long abertasDe(Usuario titular) {
        return count("titular = ?1 and status <> ?2", titular, StatusConta.ENCERRADA);
    }
}
