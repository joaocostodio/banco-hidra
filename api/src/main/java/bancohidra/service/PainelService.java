package bancohidra.service;

import bancohidra.dto.PainelDtos.Dia;
import bancohidra.dto.PainelDtos.ResumoResponse;
import bancohidra.entity.Conta;
import bancohidra.entity.Perfil;
import bancohidra.entity.StatusConta;
import bancohidra.entity.TipoTransacao;
import bancohidra.entity.Transacao;
import bancohidra.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PainelService {

    /** Quantos dias o gráfico do painel mostra (hoje incluído). */
    public static final int DIAS_GRAFICO = 7;

    /**
     * Cada operação conta uma vez: a perna de ENTRADA de depósitos e transferências,
     * ou a SAÍDA sem contraparte (dinheiro que sai do banco, como um pagamento).
     */
    private static final String UMA_LINHA_POR_OPERACAO =
            "(t.tipo = :entrada or t.contraparte is null)";

    public ResumoResponse resumo(LocalDate hoje) {
        BigDecimal total = Conta.getEntityManager()
                .createQuery("select coalesce(sum(c.saldo), 0) from Conta c", BigDecimal.class)
                .getSingleResult();

        Map<StatusConta, Long> porStatus = new EnumMap<>(StatusConta.class);
        Conta.getEntityManager()
                .createQuery("select c.status, count(c) from Conta c group by c.status", Object[].class)
                .getResultStream()
                .forEach(l -> porStatus.put((StatusConta) l[0], (Long) l[1]));

        long clientes = Usuario.count("perfil", Perfil.CLIENTE);
        long novosNoMes = Usuario.count("perfil = ?1 and criadoEm >= ?2",
                Perfil.CLIENTE, hoje.withDayOfMonth(1).atStartOfDay());
        long pendentes = Usuario.count("perfil = ?1 and trocarSenha = true", Perfil.CLIENTE);

        List<Dia> dias = ultimosDias(hoje);
        Dia diaDeHoje = dias.getLast();

        return new ResumoResponse(total, clientes, novosNoMes, pendentes,
                porStatus.getOrDefault(StatusConta.ATIVA, 0L),
                porStatus.getOrDefault(StatusConta.BLOQUEADA, 0L),
                porStatus.getOrDefault(StatusConta.ENCERRADA, 0L),
                diaDeHoje.operacoes(), diaDeHoje.valor(), dias);
    }

    /** Volume de cada um dos últimos dias, do mais antigo para hoje (dias sem movimento vêm zerados). */
    private List<Dia> ultimosDias(LocalDate hoje) {
        LocalDate primeiro = hoje.minusDays(DIAS_GRAFICO - 1L);
        List<Object[]> linhas = Transacao.getEntityManager()
                .createQuery("select t.dataHora, t.valor from Transacao t where t.dataHora >= :inicio and "
                        + UMA_LINHA_POR_OPERACAO, Object[].class)
                .setParameter("inicio", primeiro.atStartOfDay())
                .setParameter("entrada", TipoTransacao.ENTRADA)
                .getResultList();

        long[] operacoes = new long[DIAS_GRAFICO];
        BigDecimal[] valores = new BigDecimal[DIAS_GRAFICO];
        Arrays.fill(valores, BigDecimal.ZERO.setScale(2));
        for (Object[] l : linhas) {
            int i = (int) (((LocalDateTime) l[0]).toLocalDate().toEpochDay() - primeiro.toEpochDay());
            if (i >= 0 && i < DIAS_GRAFICO) {
                operacoes[i]++;
                valores[i] = valores[i].add((BigDecimal) l[1]);
            }
        }

        List<Dia> dias = new ArrayList<>(DIAS_GRAFICO);
        for (int i = 0; i < DIAS_GRAFICO; i++) {
            dias.add(new Dia(primeiro.plusDays(i), operacoes[i], valores[i]));
        }
        return dias;
    }
}
