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
 * Tudo que mexe em saldo passa por aqui: depósito e transferência (o Pix é uma transferência).
 * Cada operação roda numa transação do banco de dados: ou tudo é gravado, ou nada.
 */
@ApplicationScoped
public class MovimentacaoService {

    @Transactional
    public Transacao depositar(Long contaId, BigDecimal valor, String descricao) {
        Conta conta = travar(contaId);
        conta.depositar(valor);
        return registrar(conta, TipoTransacao.ENTRADA, valor, textoOu(descricao, "Depósito"),
                Categoria.DEPOSITO, null, UUID.randomUUID());
    }

    /**
     * Transfere entre duas contas do Banco Hidra.
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
        Transacao saida = registrar(origem, TipoTransacao.SAIDA, valor, texto, categoria, destino, autenticacao);
        registrar(destino, TipoTransacao.ENTRADA, valor, texto, categoria, origem, autenticacao);
        return saida;
    }

    private Conta travar(Long id) {
        Conta conta = Conta.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (conta == null) {
            throw new NaoEncontradoException("Conta " + id + " não encontrada");
        }
        return conta;
    }

    private Transacao registrar(Conta conta, TipoTransacao tipo, BigDecimal valor, String descricao,
                                String categoria, Conta contraparte, UUID autenticacao) {
        Transacao t = new Transacao();
        t.conta = conta;
        t.tipo = tipo;
        t.valor = valor.setScale(2);
        t.saldoApos = conta.getSaldo();
        t.descricao = descricao;
        t.categoria = Categoria.porDescricao(categoria)
                .orElseThrow(() -> new IllegalStateException("Categoria de sistema ausente: " + categoria));
        t.contraparte = contraparte;
        t.autenticacao = autenticacao;
        t.persist();
        return t;
    }

    private static String textoOu(String texto, String padrao) {
        return texto == null || texto.isBlank() ? padrao : texto.trim();
    }
}
