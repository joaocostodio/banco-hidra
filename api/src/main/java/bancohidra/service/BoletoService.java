package bancohidra.service;

import bancohidra.entity.Boleto;
import bancohidra.entity.Categoria;
import bancohidra.entity.Conta;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import bancohidra.util.CodigoBoleto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Emissão, consulta, pagamento e cancelamento de boletos entre clientes do Banco Hidra. */
@ApplicationScoped
public class BoletoService {

    @Inject
    MovimentacaoService movimentacaoService;

    @Transactional
    public Boleto emitir(Conta beneficiario, BigDecimal valor, LocalDate vencimento, String descricao) {
        Boleto boleto = Boleto.emitir(beneficiario, valor, vencimento, descricao, LocalDate.now());
        boleto.persist();
        return boleto;
    }

    /** Confere os dígitos e acha o boleto, sem pagar (tela "confira antes de pagar"). */
    public Boleto consultar(String linha) {
        return buscar(linha, false);
    }

    /**
     * Paga o valor do dia (com multa e juros, se vencido). O boleto é travado antes de mexer nas
     * contas: se duas pessoas tentarem pagar ao mesmo tempo, a segunda já o encontra pago.
     *
     * @return a transação de saída do pagador (comprovante)
     */
    @Transactional
    public Transacao pagar(Conta pagador, String linha) {
        Boleto boleto = buscar(linha, true);
        boleto.exigirAberto();
        if (boleto.beneficiario.id.equals(pagador.id)) {
            throw new RegraNegocioException("Este boleto foi emitido por você");
        }
        Boleto.Encargos encargos = boleto.encargosEm(LocalDate.now());
        String descricao = "Boleto" + (boleto.descricao == null ? "" : ": " + boleto.descricao);
        Transacao saida = movimentacaoService.transferir(pagador.id, boleto.beneficiario.id, encargos.total(),
                descricao, Categoria.PAGAMENTO);
        boleto.registrarPagamento(saida.conta, encargos.total(), saida.autenticacao, LocalDateTime.now());
        return saida;
    }

    @Transactional
    public Boleto cancelar(Conta beneficiario, Long boletoId) {
        Boleto boleto = Boleto.findById(boletoId, LockModeType.PESSIMISTIC_WRITE);
        if (boleto == null || !boleto.beneficiario.id.equals(beneficiario.id)) {
            throw new NaoEncontradoException("Boleto não encontrado");
        }
        boleto.cancelar();
        return boleto;
    }

    private static Boleto buscar(String linha, boolean travar) {
        CodigoBoleto.Lido lido = CodigoBoleto.ler(linha);
        var consulta = Boleto.<Boleto>find("codigoBarras", lido.codigoBarras());
        Boleto boleto = travar ? consulta.withLock(LockModeType.PESSIMISTIC_WRITE).firstResult() : consulta.firstResult();
        if (boleto == null) {
            throw new NaoEncontradoException("Boleto não encontrado no Banco Hidra");
        }
        return boleto;
    }
}
