package bancohidra.entity;

import bancohidra.exception.RegraNegocioException;
import bancohidra.exception.SaldoInsuficienteException;
import bancohidra.util.Dinheiro;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Conta do Banco Hidra. É <b>abstrata</b>: cada tipo é uma subclasse ({@link ContaCorrente}, {@link ContaCarteira},
 * {@link ContaPoupanca}, {@link ContaInvestimento}) que pode mudar as regras de movimentação sobrescrevendo os
 * métodos "gancho" abaixo (polimorfismo). O código que movimenta dinheiro só conversa com {@code Conta} e não
 * precisa saber o tipo.
 *
 * <p>No banco de dados é uma tabela só ({@code SINGLE_TABLE}): a coluna {@code tipo} diz qual subclasse é cada linha.
 *
 * <p>O saldo é <b>encapsulado</b>: o campo é privado, não tem setter e só muda por {@link #depositar},
 * {@link #sacar} e pelo rendimento ({@link #adicionarRendimento}, só para as subclasses).
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 15,
        columnDefinition = "varchar(15) default 'CORRENTE' not null")
public abstract class Conta extends PanacheEntity {

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

    /** Cria a conta do tipo pedido (fábrica: quem chama não precisa conhecer as subclasses). */
    public static Conta nova(TipoConta tipo, ProdutoInvestimento produto) {
        return switch (tipo) {
            case CORRENTE -> new ContaCorrente();
            case CARTEIRA -> new ContaCarteira();
            case POUPANCA -> new ContaPoupanca();
            case INVESTIMENTO -> new ContaInvestimento(produto);
        };
    }

    public abstract TipoConta getTipo();

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

    /** Rendimento pago pelo banco (só as contas remuneradas chamam). Rende mesmo bloqueada; encerrada, não. */
    protected final void adicionarRendimento(BigDecimal valor) {
        if (valor.signum() < 0 || status == StatusConta.ENCERRADA) {
            throw new IllegalStateException("Rendimento inválido para a conta " + numero);
        }
        saldo = saldo.add(valor.setScale(2, java.math.RoundingMode.HALF_EVEN));
    }

    // ---------- ganchos que cada tipo de conta pode sobrescrever (polimorfismo) ----------

    /** Pode receber esta transferência? Por padrão, de qualquer conta do banco. */
    public void exigirPodeReceberDe(Conta origem) {
        // aceita
    }

    /** Pode mandar dinheiro para esta conta? Por padrão, para qualquer conta do banco. */
    public void exigirPodeEnviarPara(Conta destino) {
        // aceita
    }

    /** Aceita lançamento manual de receita/despesa? */
    public boolean aceitaLancamento() {
        return true;
    }

    /**
     * Chamado antes de sair dinheiro numa transferência. Devolve o imposto retido (o investimento desconta o
     * Imposto de Renda do resgate); nas outras contas, zero.
     */
    public BigDecimal prepararSaida(BigDecimal valor, LocalDate hoje) {
        return BigDecimal.ZERO.setScale(2);
    }

    /** Chamado depois de entrar dinheiro (o investimento registra o valor aplicado). */
    public void aoReceber(BigDecimal valor, LocalDate hoje) {
        // nada
    }

    // ---------- situação ----------

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

    public boolean mesmoTitularQue(Conta outra) {
        return outra != null && titular.id.equals(outra.titular.id);
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
