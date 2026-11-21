package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

class PeriodoDoHistoricoTest {

    @Test
    @DisplayName("ATD-05: sem datas, o período é o mês corrente inteiro")
    void mesCorrente() {
        var periodo = PeriodoDoHistorico.de(Optional.empty(), Optional.empty(), LocalDate.of(2026, 2, 10));

        assertThat(periodo.inicio()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(periodo.fim()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("ATD-05: o último dia entra inteiro, até a meia-noite seguinte em São Paulo")
    void fimInclusivo() {
        var periodo = new PeriodoDoHistorico(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(periodo.fimNoFuso(ZoneId.of("America/Sao_Paulo")).toString())
                .isEqualTo("2026-11-01T00:00-03:00");
    }

    @Test
    @DisplayName("ATD-05: recusa fim antes do início e período maior que um ano")
    void periodoInvalido() {
        assertThatThrownBy(() -> new PeriodoDoHistorico(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> new PeriodoDoHistorico(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Consulte no máximo um ano por vez.");
    }
}
