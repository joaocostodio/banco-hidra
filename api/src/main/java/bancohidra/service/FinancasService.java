package bancohidra.service;

import bancohidra.dto.FinancasDtos.LancamentoRequest;
import bancohidra.dto.FinancasDtos.MinhaContaResponse;
import bancohidra.dto.FinancasDtos.ResumoResponse;
import bancohidra.dto.FinancasDtos.TotalCategoria;
import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.StatusConta;
import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Transacao;
import bancohidra.entity.Usuario;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestão financeira do cliente: lançamentos, categorias próprias, transações filtradas e o resumo.
 * Toda consulta parte do usuário logado: um cliente nunca enxerga conta, categoria ou transação de outro.
 */
@ApplicationScoped
public class FinancasService {

    public static final int DIAS_PADRAO = 30;
    public static final int DIAS_MAXIMO = 366;
    public static final int LIMITE_CATEGORIAS_PROPRIAS = 30;

    /**
     * Uma transação é "interna" quando a outra ponta também é conta do cliente (transferência entre as
     * próprias contas). Ela aparece no extrato, mas não conta como receita nem despesa.
     */
    private static final String NAO_INTERNA = "(cp is null or cp.titular <> :usuario)";

    @Inject
    MovimentacaoService movimentacaoService;

    @Inject
    ClienteService clienteService;

    // ---------- lançamentos ----------

    @Transactional
    public Transacao lancar(Usuario usuario, LancamentoRequest req) {
        Conta conta = clienteService.contaAbertaDo(usuario, req.contaId());
        Categoria categoria = categoriaVisivel(usuario, req.categoriaId());
        if (!categoria.podeSerUsadaPor(usuario)) {
            throw new RegraNegocioException("A categoria " + categoria.descricao + " é usada só pelo banco; escolha outra");
        }
        return movimentacaoService.lancar(conta.id, req.tipo().comoTransacao(), req.valor(), categoria,
                req.descricao());
    }

    // ---------- transações com filtro ----------

    /**
     * Transações de todas as contas do cliente no período (padrão: últimos 30 dias, máximo 1 ano), da mais
     * recente para a mais antiga. Filtros opcionais: conta, categoria e tipo (entrada/saída).
     */
    public List<Transacao> transacoes(Usuario usuario, Long contaId, Long categoriaId, TipoTransacao tipo,
                                      LocalDate inicio, LocalDate fim) {
        LocalDate ate = fim != null ? fim : LocalDate.now();
        LocalDate de = inicio != null ? inicio : ate.minusDays(DIAS_PADRAO);
        if (de.isAfter(ate)) {
            throw new RegraNegocioException("A data inicial deve ser anterior à final");
        }
        if (de.plusDays(DIAS_MAXIMO).isBefore(ate)) {
            throw new RegraNegocioException("O período pode ter no máximo 1 ano");
        }

        // Filtros montados só com parâmetros (nunca concatenando o que veio do usuário): sem SQL injection.
        StringBuilder jpql = new StringBuilder("conta.titular = :usuario and dataHora >= :inicio and dataHora < :fim");
        Parameters p = Parameters.with("usuario", usuario)
                .and("inicio", de.atStartOfDay())
                .and("fim", ate.plusDays(1).atStartOfDay());
        if (contaId != null) {
            jpql.append(" and conta = :conta");
            p.and("conta", clienteService.contaDo(usuario, contaId));
        }
        if (categoriaId != null) {
            jpql.append(" and categoria = :categoria");
            p.and("categoria", categoriaVisivel(usuario, categoriaId));
        }
        if (tipo != null) {
            jpql.append(" and tipo = :tipo");
            p.and("tipo", tipo);
        }
        return Transacao.list(jpql.toString(), Sort.descending("dataHora", "id"), p);
    }

    // ---------- resumo (tela inicial) ----------

    public ResumoResponse resumo(Usuario usuario, LocalDate hoje) {
        List<Conta> contas = Conta.doTitular(usuario).stream()
                .filter(c -> c.status != StatusConta.ENCERRADA).toList();
        Long idPrincipal = Conta.principalDe(usuario).id;
        BigDecimal total = contas.stream().map(Conta::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        return new ResumoResponse(total,
                contas.stream().map(c -> MinhaContaResponse.de(c, idPrincipal)).toList(),
                totalDoMes(usuario, TipoTransacao.ENTRADA, inicioMes),
                totalDoMes(usuario, TipoTransacao.SAIDA, inicioMes),
                despesasPorCategoria(usuario, inicioMes));
    }

    private static BigDecimal totalDoMes(Usuario usuario, TipoTransacao tipo, LocalDateTime inicioMes) {
        return Transacao.getEntityManager()
                .createQuery("select coalesce(sum(t.valor), 0) from Transacao t left join t.contraparte cp "
                        + "where t.conta.titular = :usuario and t.tipo = :tipo and t.dataHora >= :inicio and "
                        + NAO_INTERNA, BigDecimal.class)
                .setParameter("usuario", usuario)
                .setParameter("tipo", tipo)
                .setParameter("inicio", inicioMes)
                .getSingleResult();
    }

    private static List<TotalCategoria> despesasPorCategoria(Usuario usuario, LocalDateTime inicioMes) {
        return Transacao.getEntityManager()
                .createQuery("select new bancohidra.dto.FinancasDtos$TotalCategoria(t.categoria.id, "
                        + "t.categoria.descricao, sum(t.valor)) from Transacao t left join t.contraparte cp "
                        + "where t.conta.titular = :usuario and t.tipo = :saida and t.dataHora >= :inicio and "
                        + NAO_INTERNA + " group by t.categoria.id, t.categoria.descricao order by sum(t.valor) desc",
                        TotalCategoria.class)
                .setParameter("usuario", usuario)
                .setParameter("saida", TipoTransacao.SAIDA)
                .setParameter("inicio", inicioMes)
                .getResultList();
    }

    // ---------- categorias próprias ----------

    @Transactional
    public Categoria criarCategoria(Usuario usuario, String descricao) {
        if (Categoria.count("dono", usuario) >= LIMITE_CATEGORIAS_PROPRIAS) {
            throw new RegraNegocioException("Limite de " + LIMITE_CATEGORIAS_PROPRIAS + " categorias próprias atingido");
        }
        exigirDescricaoLivre(usuario, descricao, null);
        Categoria categoria = new Categoria();
        categoria.descricao = descricao.trim();
        categoria.dono = usuario;
        categoria.persist();
        return categoria;
    }

    @Transactional
    public Categoria renomearCategoria(Usuario usuario, Long id, String descricao) {
        Categoria categoria = categoriaPropria(usuario, id);
        exigirDescricaoLivre(usuario, descricao, id);
        categoria.descricao = descricao.trim();
        return categoria;
    }

    @Transactional
    public void excluirCategoria(Usuario usuario, Long id) {
        Categoria categoria = categoriaPropria(usuario, id);
        if (Transacao.count("categoria", categoria) > 0) {
            throw new RegraNegocioException("Categoria em uso por transações não pode ser excluída");
        }
        categoria.delete();
    }

    /** Categoria que o cliente enxerga (padrão, de sistema ou dele). A de outro cliente "não existe". */
    public Categoria categoriaVisivel(Usuario usuario, Long id) {
        Categoria categoria = id == null ? null : Categoria.findById(id);
        if (categoria == null || (categoria.dono != null && !categoria.isPropriaDe(usuario))) {
            throw new NaoEncontradoException("Categoria não encontrada");
        }
        return categoria;
    }

    private static Categoria categoriaPropria(Usuario usuario, Long id) {
        Categoria categoria = id == null ? null : Categoria.findById(id);
        if (categoria == null || !categoria.isPropriaDe(usuario)) {
            throw new NaoEncontradoException("Categoria não encontrada");
        }
        return categoria;
    }

    private static void exigirDescricaoLivre(Usuario usuario, String descricao, Long idAtual) {
        if (Categoria.descricaoEmUso(descricao, usuario, idAtual)) {
            throw new RegraNegocioException("Você já tem uma categoria chamada " + descricao.trim());
        }
    }
}
