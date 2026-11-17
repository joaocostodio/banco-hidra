package bancohidra.service;

import bancohidra.entity.ContaRemunerada;
import bancohidra.entity.StatusConta;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Paga os rendimentos da poupança e dos investimentos. Roda sozinho todo dia de madrugada; o administrador
 * também pode simular a passagem de 1 mês (para mostrar funcionando sem esperar).
 */
@ApplicationScoped
public class RendimentoService {

    @Inject
    MovimentacaoService movimentacaoService;

    /** Quantas contas renderam e quanto o banco pagou no total. */
    public record Resultado(int contas, BigDecimal total) {
    }

    /** Todo dia às 00:10. */
    @Scheduled(cron = "0 10 0 * * ?", identity = "rendimentos-diarios")
    void diario() {
        Resultado r = pagar(LocalDate.now(), false);
        Log.infof("Rendimentos do dia: %d contas, R$ %s", r.contas(), r.total());
    }

    /** Paga o que estiver devido até hoje (em dia, normalmente não há nada além do rendimento diário). */
    public Resultado pagarDevidos() {
        return pagar(LocalDate.now(), false);
    }

    /** Como se cada conta remunerada tivesse ficado 1 mês sem receber rendimento (só o administrador). */
    public Resultado simularUmMes() {
        return pagar(LocalDate.now(), true);
    }

    private Resultado pagar(LocalDate hoje, boolean simularUmMes) {
        List<Long> ids = ContaRemunerada.<ContaRemunerada>list("status <> ?1", StatusConta.ENCERRADA)
                .stream().map(c -> c.id).toList();
        int contas = 0;
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Long id : ids) {
            try {
                BigDecimal valor = movimentacaoService.render(id, hoje, simularUmMes);
                if (valor.signum() > 0) {
                    contas++;
                    total = total.add(valor);
                }
            } catch (RuntimeException e) {
                // Uma conta com problema não impede as outras de render (cada uma tem a sua transação).
                Log.errorf(e, "Falha ao pagar o rendimento da conta %d", id);
            }
        }
        return new Resultado(contas, total);
    }
}
