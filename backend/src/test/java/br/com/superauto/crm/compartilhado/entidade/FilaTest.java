package br.com.superauto.crm.compartilhado.entidade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

class FilaTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 31);

    @Test
    @DisplayName("RG-14: próximo contato antes de hoje é Atrasado, hoje é Hoje, depois é Futuro, sem data é Sem data")
    void filaPelaAgenda() {
        assertThat(Fila.daAgenda(Optional.of(HOJE.minusDays(1)), HOJE)).isEqualTo(Fila.ATRASADO);
        assertThat(Fila.daAgenda(Optional.of(HOJE), HOJE)).isEqualTo(Fila.HOJE);
        assertThat(Fila.daAgenda(Optional.of(HOJE.plusDays(1)), HOJE)).isEqualTo(Fila.FUTURO);
        assertThat(Fila.daAgenda(Optional.empty(), HOJE)).isEqualTo(Fila.SEM_DATA);
    }

    @Test
    @DisplayName("FIL-03: os dias de atraso contam a partir do próximo contato; quem não venceu tem zero")
    void diasDeAtraso() {
        assertThat(Fila.diasDeAtraso(Optional.of(LocalDate.of(2026, 3, 20)), HOJE)).isEqualTo(225);
        assertThat(Fila.diasDeAtraso(Optional.of(HOJE.plusDays(3)), HOJE)).isZero();
        assertThat(Fila.diasDeAtraso(Optional.empty(), HOJE)).isZero();
    }
}
