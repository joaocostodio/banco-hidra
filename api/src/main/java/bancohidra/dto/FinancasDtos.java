package bancohidra.dto;

import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.StatusConta;
import bancohidra.entity.TipoConta;
import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Gestão financeira do cliente: as contas dele, lançamentos, categorias próprias e o resumo da tela inicial. */
public final class FinancasDtos {

    private FinancasDtos() {
    }

    // ---------- contas ----------

    public record ContaRequest(
            @NotBlank(message = "Dê um nome para a conta") @Size(max = 40, message = "Nome com no máximo 40 caracteres")
            String nome,
            @NotNull(message = "Escolha o tipo da conta") TipoConta tipo) {
    }

    /** {@code principal}: a primeira conta do cliente, a das chaves Pix e dos boletos (não pode ser encerrada). */
    public record MinhaContaResponse(Long id, String nome, TipoConta tipo, String agencia, String numero,
                                     BigDecimal saldo, StatusConta status, boolean principal,
                                     LocalDateTime abertaEm) {
        public static MinhaContaResponse de(Conta c, Long idPrincipal) {
            return new MinhaContaResponse(c.id, c.nome, c.tipo, c.agencia, c.numero, c.getSaldo(), c.status,
                    c.id.equals(idPrincipal), c.abertaEm);
        }
    }

    // ---------- lançamentos ----------

    /** Receita entra no saldo; despesa sai. */
    public enum TipoLancamento {
        RECEITA,
        DESPESA;

        public TipoTransacao comoTransacao() {
            return this == RECEITA ? TipoTransacao.ENTRADA : TipoTransacao.SAIDA;
        }
    }

    public record LancamentoRequest(
            @NotNull(message = "Escolha a conta") Long contaId,
            @NotNull(message = "Escolha receita ou despesa") TipoLancamento tipo,
            @NotNull(message = "Informe o valor") BigDecimal valor,
            @NotNull(message = "Escolha a categoria") Long categoriaId,
            @Size(max = 200, message = "Descrição com no máximo 200 caracteres") String descricao) {
    }

    // ---------- categorias ----------

    public record CategoriaPropriaRequest(
            @NotBlank(message = "Informe o nome da categoria")
            @Size(max = 60, message = "Nome com no máximo 60 caracteres") String descricao) {
    }

    /**
     * {@code propria}: criada pelo cliente (ele pode editar e excluir).
     * {@code sistema}: usada pelo próprio banco (Pix, Transferência...): serve para filtrar, não para lançar.
     */
    public record CategoriaClienteResponse(Long id, String descricao, boolean propria, boolean sistema) {
        public static CategoriaClienteResponse de(Categoria c, Usuario usuario) {
            return new CategoriaClienteResponse(c.id, c.descricao, c.isPropriaDe(usuario), c.sistema);
        }
    }

    // ---------- tela inicial ----------

    /**
     * Saldo de cada conta aberta e o total, mais as receitas e despesas do mês. Transferências entre as
     * contas do próprio cliente não contam como receita nem despesa (o dinheiro só mudou de lugar).
     */
    public record ResumoResponse(BigDecimal saldoTotal, List<MinhaContaResponse> contas, BigDecimal receitasMes,
                                 BigDecimal despesasMes, List<TotalCategoria> despesasPorCategoria) {
    }

    public record TotalCategoria(Long categoriaId, String categoria, BigDecimal total) {
    }
}
