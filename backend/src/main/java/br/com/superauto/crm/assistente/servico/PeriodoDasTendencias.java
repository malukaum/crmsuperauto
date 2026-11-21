package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.erro.NegocioException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/** IA-03 · O período do painel de tendências, com os dois dias incluídos. Sem datas, os últimos 30 dias. */
public record PeriodoDasTendencias(LocalDate inicio, LocalDate fim) {
    private static final int DIAS_PADRAO = 30;
    private static final int MAXIMO_DE_MESES = 12;

    public PeriodoDasTendencias {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (fim.isBefore(inicio))
            throw NegocioException.invalido("periodo-invalido", "A data final não pode ser anterior à inicial.");
        if (fim.isAfter(inicio.plusMonths(MAXIMO_DE_MESES)))
            throw NegocioException.invalido("periodo-longo", "Consulte no máximo um ano por vez.");
    }

    public static PeriodoDasTendencias de(Optional<LocalDate> inicio, Optional<LocalDate> fim, LocalDate hoje) {
        LocalDate ate = fim.orElse(hoje);
        return new PeriodoDasTendencias(inicio.orElse(ate.minusDays(DIAS_PADRAO - 1L)), ate);
    }

    OffsetDateTime inicioNoFuso(ZoneId fuso) {
        return inicio.atStartOfDay(fuso).toOffsetDateTime();
    }

    OffsetDateTime fimNoFuso(ZoneId fuso) {
        return fim.plusDays(1).atStartOfDay(fuso).toOffsetDateTime();
    }
}
