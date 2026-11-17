package bancohidra.service;

import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.ContaRemunerada;
import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import bancohidra.util.Dinheiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Tudo que mexe em saldo passa por aqui: depósito, lançamento de receita/despesa e transferência
 * (o Pix e o pagamento de boleto são transferências). Cada operação roda numa transação do banco
 * de dados: ou tudo é gravado, ou nada. As contas são travadas (lock) antes de mudar o saldo, para
 * duas operações ao mesmo tempo não lerem o mesmo saldo antigo.
 */
@ApplicationScoped
public class MovimentacaoService {

    @Transactional
    public Transacao depositar(Long contaId, BigDecimal valor, String descricao) {
        Conta conta = travar(contaId);
        conta.depositar(valor);
        conta.aoReceber(valor, LocalDate.now());
        return registrar(conta, TipoTransacao.ENTRADA, valor, textoOu(descricao, Categoria.DEPOSITO),
                sistema(Categoria.DEPOSITO), null, UUID.randomUUID());
    }

    /**
     * Paga o rendimento devido até {@code hoje} numa conta remunerada (poupança ou investimento) e registra no
     * extrato. Com {@code simularUmMes}, antes "volta o relógio" da conta 1 mês (só o administrador, para a
     * apresentação). Cada conta roda na sua própria transação: um erro numa não desfaz as outras.
     *
     * @return o valor creditado (zero se não havia o que pagar ou a conta não rende)
     */
    @Transactional
    public BigDecimal render(Long contaId, LocalDate hoje, boolean simularUmMes) {
        if (!(travar(contaId) instanceof ContaRemunerada conta)) {
            return BigDecimal.ZERO;
        }
        if (simularUmMes) {
            conta.recuarRelogio(Period.ofMonths(1));
        }
        BigDecimal valor = conta.render(hoje);
        if (valor.signum() > 0) {
            registrar(conta, TipoTransacao.ENTRADA, valor, conta.descricaoRendimento(),
                    sistema(Categoria.RENDIMENTO), null, UUID.randomUUID());
        }
        return valor;
    }

    /**
     * Lançamento feito pelo cliente na própria conta: uma receita (salário, venda...) entra no saldo;
     * uma despesa (mercado, aluguel...) sai, se houver saldo.
     * Quem chama garante que a conta e a categoria são do cliente.
     */
    @Transactional
    public Transacao lancar(Long contaId, TipoTransacao tipo, BigDecimal valor, Categoria categoria,
                            String descricao) {
        Conta conta = travar(contaId);
        if (!conta.aceitaLancamento()) {
            throw new RegraNegocioException("A conta " + conta.nome + " não aceita lançamentos: "
                    + "movimente com transferências entre as suas contas");
        }
        if (tipo == TipoTransacao.ENTRADA) {
            conta.depositar(valor);
            conta.aoReceber(valor, LocalDate.now());
        } else {
            conta.sacar(valor); // lança exceção se faltar saldo ou a conta não estiver ativa
        }
        return registrar(conta, tipo, valor, textoOu(descricao, categoria.descricao), categoria, null,
                UUID.randomUUID());
    }

    /**
     * Transfere entre duas contas do Banco Hidra (de clientes diferentes ou do mesmo cliente).
     *
     * @return a transação de saída (da conta de origem), que serve de comprovante
     */
    @Transactional
    public Transacao transferir(Long origemId, Long destinoId, BigDecimal valor, String descricao,
                                String categoria) {
        if (origemId.equals(destinoId)) {
            throw new RegraNegocioException("A conta de destino deve ser diferente da de origem");
        }

        // Trava as duas contas sempre na mesma ordem (menor id primeiro). Assim duas transferências
        // simultâneas A→B e B→A não ficam esperando uma pela outra para sempre (deadlock).
        Conta primeira = travar(Math.min(origemId, destinoId));
        Conta segunda = travar(Math.max(origemId, destinoId));
        Conta origem = primeira.id.equals(origemId) ? primeira : segunda;
        Conta destino = origem == primeira ? segunda : primeira;

        if (!destino.isAtiva()) {
            throw new RegraNegocioException("A conta de destino não está recebendo transferências");
        }
        // Regras de cada tipo de conta (polimorfismo): ex.: o investimento só aplica e resgata com o próprio dono.
        origem.exigirPodeEnviarPara(destino);
        destino.exigirPodeReceberDe(origem);

        // O investimento desconta o IR do resgate: o destino recebe o líquido, e o imposto sai à parte no extrato.
        LocalDate hoje = LocalDate.now();
        BigDecimal imposto = origem.prepararSaida(Dinheiro.exigirPositivo(valor), hoje);
        BigDecimal liquido = valor.subtract(imposto);
        if (liquido.signum() <= 0) {
            throw new RegraNegocioException("Valor pequeno demais: o imposto consumiria o resgate inteiro");
        }
        origem.sacar(liquido);    // lança exceção se faltar saldo ou a conta não estiver ativa
        destino.depositar(liquido);
        destino.aoReceber(liquido, hoje);

        UUID autenticacao = UUID.randomUUID();
        String texto = textoOu(descricao, categoria);
        Categoria cat = sistema(categoria);
        Transacao saida = registrar(origem, TipoTransacao.SAIDA, liquido, texto, cat, destino, autenticacao);
        registrar(destino, TipoTransacao.ENTRADA, liquido, texto, cat, origem, autenticacao);
        if (imposto.signum() > 0) {
            origem.sacar(imposto);
            registrar(origem, TipoTransacao.SAIDA, imposto, "Imposto de Renda retido no resgate",
                    sistema(Categoria.IMPOSTOS), null, UUID.randomUUID());
        }
        return saida;
    }

    /**
     * Trava a linha da conta no banco (SELECT ... FOR UPDATE) até o fim da transação e <b>relê</b> o saldo.
     * Só travar não basta: se a conta já tinha sido carregada antes nesta transação (ex.: para conferir de
     * quem ela é), o Hibernate devolveria o objeto da memória com o saldo antigo, e duas operações
     * simultâneas poderiam partir do mesmo saldo (uma "apagaria" a outra).
     */
    private Conta travar(Long id) {
        Conta conta = Conta.findById(id);
        if (conta == null) {
            throw new NaoEncontradoException("Conta " + id + " não encontrada");
        }
        Conta.getEntityManager().refresh(conta, LockModeType.PESSIMISTIC_WRITE);
        return conta;
    }

    private static Categoria sistema(String descricao) {
        return Categoria.globalPorDescricao(descricao)
                .orElseThrow(() -> new IllegalStateException("Categoria de sistema ausente: " + descricao));
    }

    private static Transacao registrar(Conta conta, TipoTransacao tipo, BigDecimal valor, String descricao,
                                       Categoria categoria, Conta contraparte, UUID autenticacao) {
        Transacao t = new Transacao();
        t.conta = conta;
        t.tipo = tipo;
        t.valor = valor.setScale(2);
        t.saldoApos = conta.getSaldo();
        t.descricao = descricao;
        t.categoria = categoria;
        t.contraparte = contraparte;
        t.autenticacao = autenticacao;
        t.persist();
        return t;
    }

    private static String textoOu(String texto, String padrao) {
        return texto == null || texto.isBlank() ? padrao : texto.trim();
    }
}
