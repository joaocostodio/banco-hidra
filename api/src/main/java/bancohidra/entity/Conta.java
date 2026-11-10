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

    public static Conta doTitular(Usuario titular) {
        return find("titular", titular).firstResult();
    }
}
