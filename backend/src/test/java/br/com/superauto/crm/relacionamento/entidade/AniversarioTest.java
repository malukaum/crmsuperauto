package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.*;

import java.time.LocalDate;

class AniversarioTest {
    @Test
    @DisplayName("FIL-04: nascido em 29/02/1992 aparece em 28/02/2027")
    void fevereiroComum() {
        assertThat(Aniversario.proximo(LocalDate.of(1992, 2, 29), LocalDate.of(2027, 2, 28)))
                .isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    @DisplayName("Na virada para ano bissexto, a data volta a ser 29/02")
    void proximoAnoBissexto() {
        assertThat(Aniversario.proximo(LocalDate.of(1992, 2, 29), LocalDate.of(2027, 3, 1)))
                .isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("Aniversário que já passou é mostrado no ano seguinte")
    void viradaDoAno() {
        assertThat(Aniversario.proximo(LocalDate.of(1992, 1, 2), LocalDate.of(2026, 12, 31)))
                .isEqualTo(LocalDate.of(2027, 1, 2));
    }
}
