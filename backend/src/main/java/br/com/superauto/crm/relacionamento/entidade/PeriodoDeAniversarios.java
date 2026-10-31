package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/** Janela inclusiva das abas de aniversariantes, com a exceção de 29/02 (FIL-04). */
public record PeriodoDeAniversarios(LocalDate hoje, int dias) {
    public static final List<Integer> PERIODOS = List.of(0, 7, 30);

    public PeriodoDeAniversarios {
        if (!PERIODOS.contains(dias))
            throw NegocioException.invalido("periodo-invalido", "Escolha Hoje, 7 ou 30 dias.");
    }

    public Set<MonthDay> nascimentosElegiveis() {
        Set<MonthDay> datas = new LinkedHashSet<>();
        IntStream.rangeClosed(0, dias)
                .mapToObj(hoje::plusDays)
                .forEach(
                        data -> {
                            datas.add(MonthDay.from(data));
                            if (!data.isLeapYear()
                                    && MonthDay.from(data).equals(MonthDay.of(2, 28)))
                                datas.add(MonthDay.of(2, 29));
                        });
        return Set.copyOf(datas);
    }
}
