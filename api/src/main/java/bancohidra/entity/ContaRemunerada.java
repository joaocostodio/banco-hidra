package bancohidra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * Conta que rende: a poupança e o investimento. Usa o padrão <b>método-modelo</b> (template method):
 * {@link #render} é igual para todas (descobre desde quando pagar, credita e guarda a data), e cada subclasse só
 * diz <i>quanto</i> rende e até quando, em {@link #calcularRendimento}.
 */
@Entity
public abstract class ContaRemunerada extends Conta {

    /** Até quando o rendimento já foi pago (nulo: desde a abertura). */
    @Column(name = "ultimo_rendimento")
    protected LocalDate ultimoRendimento;

    /** Quanto rendeu entre {@code desde} e {@code hoje}, e até que data esse cálculo vai. */
    public record Rendimento(BigDecimal valor, LocalDate ate) {
    }

    /** Cada tipo calcula do seu jeito (polimorfismo). */
    protected abstract Rendimento calcularRendimento(LocalDate desde, LocalDate hoje);

    /** Texto da linha do extrato, ex.: "Rendimento da poupança". */
    public abstract String descricaoRendimento();

    /** Quando cai o próximo rendimento (para a tela). */
    public abstract LocalDate proximoRendimento(LocalDate hoje);

    /**
     * Paga o rendimento devido até hoje e devolve quanto foi (zero se ainda não há o que pagar).
     * Método-modelo: o "como" é igual para todos; o "quanto" é de cada subclasse.
     */
    public final BigDecimal render(LocalDate hoje) {
        if (status == StatusConta.ENCERRADA) {
            return BigDecimal.ZERO.setScale(2);
        }
        LocalDate desde = dataBase();
        Rendimento r = calcularRendimento(desde, hoje);
        if (r.valor().signum() > 0) {
            adicionarRendimento(r.valor());
        }
        ultimoRendimento = r.ate();
        return r.valor().setScale(2);
    }

    /**
     * "Volta o relógio" da conta, como se o último rendimento tivesse sido pago {@code periodo} antes.
     * Usado só pelo administrador para simular a passagem do tempo na apresentação.
     */
    public void recuarRelogio(Period periodo) {
        ultimoRendimento = dataBase().minus(periodo);
    }

    protected LocalDate dataBase() {
        return ultimoRendimento != null ? ultimoRendimento : abertaEm.toLocalDate();
    }

    public LocalDate getUltimoRendimento() {
        return ultimoRendimento;
    }
}
