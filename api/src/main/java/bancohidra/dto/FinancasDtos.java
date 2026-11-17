package bancohidra.dto;

import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.ContaInvestimento;
import bancohidra.entity.ContaPoupanca;
import bancohidra.entity.ProdutoInvestimento;
import bancohidra.entity.StatusConta;
import bancohidra.entity.TipoConta;
import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Gestão financeira do cliente: as contas dele, lançamentos, categorias próprias e o resumo da tela inicial. */
public final class FinancasDtos {

    private FinancasDtos() {
    }

    // ---------- contas ----------

    /** Abrir conta: o investimento exige o {@code produto}; nos outros tipos ele é ignorado. Sem nome: o do tipo. */
    public record ContaRequest(
            @Size(max = 40, message = "Nome com no máximo 40 caracteres") String nome,
            @NotNull(message = "Escolha o tipo da conta") TipoConta tipo,
            ProdutoInvestimento produto) {
    }

    /** Renomear: o tipo não muda depois de aberta (cada tipo é uma classe diferente). */
    public record RenomearContaRequest(
            @NotBlank(message = "Dê um nome para a conta") @Size(max = 40, message = "Nome com no máximo 40 caracteres")
            String nome) {
    }

    public record MetaRequest(
            @NotNull(message = "Informe quanto quer guardar") BigDecimal valor,
            @Size(max = 60, message = "Descrição com no máximo 60 caracteres") String descricao,
            LocalDate data) {
    }

    /**
     * Uma conta do cliente. {@code principal}: a primeira, a das chaves Pix e dos boletos (não pode ser encerrada).
     * {@code rendimento} vem na poupança e no investimento; {@code meta}, na poupança com meta;
     * {@code investimento}, só no investimento.
     */
    public record MinhaContaResponse(Long id, String nome, TipoConta tipo, String agencia, String numero,
                                     BigDecimal saldo, StatusConta status, boolean principal,
                                     LocalDateTime abertaEm, RendimentoInfo rendimento, MetaInfo meta,
                                     InvestimentoInfo investimento) {
        public static MinhaContaResponse de(Conta c, Long idPrincipal) {
            LocalDate hoje = LocalDate.now();
            RendimentoInfo rendimento = null;
            MetaInfo meta = null;
            InvestimentoInfo investimento = null;
            if (c instanceof ContaPoupanca p) {
                rendimento = new RendimentoInfo("0,5% ao mês, no dia " + p.proximoRendimento(hoje).getDayOfMonth(),
                        p.proximoRendimento(hoje));
                if (p.temMeta()) {
                    meta = new MetaInfo(p.getMetaDescricao(), p.getMetaValor(), p.getMetaData(), p.progressoDaMeta());
                }
            } else if (c instanceof ContaInvestimento i) {
                ProdutoInvestimento produto = i.getProduto();
                rendimento = new RendimentoInfo(percentual(produto.taxaAnual()) + " ao ano, todo dia",
                        i.proximoRendimento(hoje));
                investimento = new InvestimentoInfo(produto, produto.getNome(), produto.taxaAnual(),
                        produto.isIsentoIR(), i.getValorAplicado(), i.getLucro(), i.aliquotaIR(hoje),
                        i.irSeResgatarTudo(hoje));
            }
            return new MinhaContaResponse(c.id, c.nome, c.getTipo(), c.agencia, c.numero, c.getSaldo(), c.status,
                    c.id.equals(idPrincipal), c.abertaEm, rendimento, meta, investimento);
        }

        private static String percentual(BigDecimal taxa) {
            return taxa.movePointRight(2).stripTrailingZeros().toPlainString().replace('.', ',') + "%";
        }
    }

    /** Regra do rendimento em texto ("0,5% ao mês, no dia 16") e quando cai o próximo. */
    public record RendimentoInfo(String regra, LocalDate proximo) {
    }

    /** {@code progresso}: de 0 a 1. */
    public record MetaInfo(String descricao, BigDecimal valor, LocalDate data, BigDecimal progresso) {
    }

    /**
     * {@code aplicado}: dinheiro que entrou; {@code lucro}: o que rendeu; {@code aliquotaIR}: a de hoje pela tabela
     * regressiva; {@code irSeResgatarTudo}: quanto de imposto sairia num resgate total hoje.
     */
    public record InvestimentoInfo(ProdutoInvestimento produto, String produtoNome, BigDecimal taxaAnual,
                                   boolean isentoIR, BigDecimal aplicado, BigDecimal lucro, BigDecimal aliquotaIR,
                                   BigDecimal irSeResgatarTudo) {
    }

    /** Produtos que o cliente pode escolher ao abrir um investimento. */
    public record ProdutoResponse(ProdutoInvestimento id, String nome, BigDecimal taxaAnual, boolean isentoIR) {
        public static ProdutoResponse de(ProdutoInvestimento p) {
            return new ProdutoResponse(p, p.getNome(), p.taxaAnual(), p.isIsentoIR());
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
