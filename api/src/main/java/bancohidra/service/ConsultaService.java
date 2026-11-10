package bancohidra.service;

import bancohidra.dto.TransacaoDtos.ComprovanteResponse;
import bancohidra.dto.TransacaoDtos.TransacaoResponse;
import bancohidra.entity.Conta;
import bancohidra.entity.Transacao;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Consultas usadas tanto pelo painel do administrador quanto pelo app do cliente. */
@ApplicationScoped
public class ConsultaService {

    public static final int DIAS_PADRAO = 30;
    public static final int DIAS_MAXIMO = 366;

    /** Extrato do período (padrão: últimos 30 dias), do mais recente para o mais antigo; o dia final entra inteiro. */
    public List<TransacaoResponse> extrato(Conta conta, LocalDate inicio, LocalDate fim) {
        LocalDate ate = fim != null ? fim : LocalDate.now();
        LocalDate de = inicio != null ? inicio : ate.minusDays(DIAS_PADRAO);
        if (de.isAfter(ate)) {
            throw new RegraNegocioException("A data inicial deve ser anterior à final");
        }
        if (de.plusDays(DIAS_MAXIMO).isBefore(ate)) {
            throw new RegraNegocioException("O período do extrato pode ter no máximo 1 ano");
        }
        return Transacao.extrato(conta, de.atStartOfDay(), ate.plusDays(1).atStartOfDay())
                .stream().map(TransacaoResponse::de).toList();
    }

    /**
     * Comprovante pela autenticação. Se {@code somenteDaConta} for informado, só devolve quando
     * essa conta participou (o cliente não pode ver comprovante dos outros).
     */
    public ComprovanteResponse comprovante(UUID autenticacao, Conta somenteDaConta) {
        List<Transacao> pernas = Transacao.porAutenticacao(autenticacao);
        boolean permitido = somenteDaConta == null
                || pernas.stream().anyMatch(t -> t.conta.id.equals(somenteDaConta.id));
        if (pernas.isEmpty() || !permitido) {
            throw new NaoEncontradoException("Comprovante não encontrado");
        }
        return ComprovanteResponse.de(pernas);
    }
}
