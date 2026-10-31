package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.*;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.MonthDay;

class PeriodoDeAniversariosTest {
    @Test
    @DisplayName("FIL-04: consulta de 28/02 em ano comum inclui quem nasceu em 29/02")
    void fevereiroComum() {
        assertThat(new PeriodoDeAniversarios(LocalDate.of(2027, 2, 28), 0).nascimentosElegiveis())
                .containsExactlyInAnyOrder(MonthDay.of(2, 28), MonthDay.of(2, 29));
    }

    @Test
    @DisplayName("FIL-04: consulta de 28/02 em ano bissexto preserva o aniversário em 29/02")
    void fevereiroBissexto() {
        assertThat(new PeriodoDeAniversarios(LocalDate.of(2028, 2, 28), 0).nascimentosElegiveis())
                .containsExactly(MonthDay.of(2, 28));
    }

    @Test
    @DisplayName("FIL-04: próximos sete dias incluem a virada do ano e a data limite")
    void viradaDoAno() {
        assertThat(new PeriodoDeAniversarios(LocalDate.of(2026, 12, 28), 7).nascimentosElegiveis())
                .hasSize(8)
                .contains(MonthDay.of(12, 28), MonthDay.of(1, 4))
                .doesNotContain(MonthDay.of(1, 5));
    }

    @Test
    @DisplayName("FIL-04: somente as janelas Hoje, 7 e 30 dias são aceitas")
    void periodoInvalido() {
        assertThatThrownBy(() -> new PeriodoDeAniversarios(LocalDate.of(2026, 10, 31), 8))
                .isInstanceOf(NegocioException.class);
    }
}
