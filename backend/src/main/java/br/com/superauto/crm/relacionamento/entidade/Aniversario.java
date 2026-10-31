package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.time.MonthDay;

/** Calendário do relacionamento: 29/02 é celebrado em 28/02 nos anos comuns. */
public final class Aniversario {
    private Aniversario() {}

    public static LocalDate proximo(LocalDate nascimento, LocalDate hoje) {
        LocalDate data = noAno(nascimento, hoje.getYear());
        return data.isBefore(hoje) ? noAno(nascimento, hoje.getYear() + 1) : data;
    }

    private static LocalDate noAno(LocalDate nascimento, int ano) {
        return MonthDay.from(nascimento).atYear(ano);
    }
}
