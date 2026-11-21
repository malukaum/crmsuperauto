package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * ATD-05 · Período consultado no histórico de atendimentos, com os dois dias incluídos. Sem datas, é o mês corrente
 * (RG-01); no máximo um ano por consulta, para a lista e a exportação continuarem leves.
 */
public record PeriodoDoHistorico(LocalDate inicio, LocalDate fim) {
    private static final int MAXIMO_DE_MESES = 12;

    public PeriodoDoHistorico {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (fim.isBefore(inicio))
            throw NegocioException.invalido(
                    "periodo-invalido", "A data final não pode ser anterior à inicial.");
        if (fim.isAfter(inicio.plusMonths(MAXIMO_DE_MESES)))
            throw NegocioException.invalido(
                    "periodo-longo", "Consulte no máximo um ano por vez.");
    }

    /** Datas vazias assumem o primeiro e o último dia do mês de hoje. */
    public static PeriodoDoHistorico de(Optional<LocalDate> inicio, Optional<LocalDate> fim, LocalDate hoje) {
        return new PeriodoDoHistorico(
                inicio.orElse(hoje.withDayOfMonth(1)),
                fim.orElse(hoje.withDayOfMonth(hoje.lengthOfMonth())));
    }

    public OffsetDateTime inicioNoFuso(ZoneId fuso) {
        return inicio.atStartOfDay(fuso).toOffsetDateTime();
    }

    /** Exclusivo: a meia-noite do dia seguinte ao fim. */
    public OffsetDateTime fimNoFuso(ZoneId fuso) {
        return fim.plusDays(1).atStartOfDay(fuso).toOffsetDateTime();
    }
}
