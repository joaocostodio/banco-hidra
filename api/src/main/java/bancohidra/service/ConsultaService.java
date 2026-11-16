package bancohidra.service;

import bancohidra.dto.TransacaoDtos.ComprovanteResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.entity.Conta;
import bancohidra.entity.Transacao;
import bancohidra.entity.Usuario;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.util.Periodo;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Consultas do painel do administrador (extrato de qualquer conta) e comprovantes. */
@ApplicationScoped
public class ConsultaService {

    /** Extrato de uma conta no período (padrão: últimos 30 dias), do mais recente para o mais antigo. */
    public List<TransacaoResponse> extrato(Conta conta, LocalDate inicio, LocalDate fim) {
        Periodo p = Periodo.de(inicio, fim, LocalDate.now());
        return Transacao.extrato(conta, p.inicio(), p.fim()).stream().map(TransacaoResponse::de).toList();
    }

    /**
     * Comprovante pela autenticação. Se {@code somenteDoTitular} for informado, só devolve quando uma
     * conta dele participou (o cliente não pode ver comprovante dos outros). O administrador passa null.
     */
    public ComprovanteResponse comprovante(UUID autenticacao, Usuario somenteDoTitular) {
        List<Transacao> pernas = Transacao.porAutenticacao(autenticacao);
        boolean permitido = somenteDoTitular == null
                || pernas.stream().anyMatch(t -> t.conta.titular.id.equals(somenteDoTitular.id));
        if (pernas.isEmpty() || !permitido) {
            throw new NaoEncontradoException("Comprovante não encontrado");
        }
        return ComprovanteResponse.de(pernas);
    }
}
