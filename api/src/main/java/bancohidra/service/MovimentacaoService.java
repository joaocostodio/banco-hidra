package bancohidra.service;

import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
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
        return registrar(conta, TipoTransacao.ENTRADA, valor, textoOu(descricao, Categoria.DEPOSITO),
                sistema(Categoria.DEPOSITO), null, UUID.randomUUID());
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
        if (tipo == TipoTransacao.ENTRADA) {
            conta.depositar(valor);
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
        origem.sacar(valor);      // lança exceção se faltar saldo ou a conta não estiver ativa
        destino.depositar(valor);

        UUID autenticacao = UUID.randomUUID();
        String texto = textoOu(descricao, categoria);
        Categoria cat = sistema(categoria);
        Transacao saida = registrar(origem, TipoTransacao.SAIDA, valor, texto, cat, destino, autenticacao);
        registrar(destino, TipoTransacao.ENTRADA, valor, texto, cat, origem, autenticacao);
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
