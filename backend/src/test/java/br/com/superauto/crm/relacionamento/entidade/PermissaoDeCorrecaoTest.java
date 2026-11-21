package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

class PermissaoDeCorrecaoTest {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final UUID AUTOR = UUID.randomUUID();
    private static final ZonedDateTime REGISTRO = LocalDateTime.of(2026, 10, 30, 22, 0).atZone(BRASILIA);
    private static final PermissaoDeCorrecao PERMISSAO = new PermissaoDeCorrecao(AUTOR, REGISTRO.toOffsetDateTime());

    @Test
    @DisplayName("ATD-04: o autor pode corrigir até o fim do dia seguinte, no dia de Brasília")
    void prazoDoAutor() {
        var autor = new ResponsavelPelaCorrecao(AUTOR, false);

        assertThat(PERMISSAO.permite(autor, REGISTRO.plusDays(1).withHour(23).withMinute(59))).isTrue();
        assertThat(PERMISSAO.permite(autor, REGISTRO.plusDays(2).withHour(0).withMinute(0))).isFalse();
    }

    @Test
    @DisplayName("ATD-04: o gerencial pode sempre; outra pessoa nunca")
    void gerencialEOutros() {
        assertThat(PERMISSAO.permite(new ResponsavelPelaCorrecao(UUID.randomUUID(), true), REGISTRO.plusYears(1)))
                .isTrue();
        assertThat(PERMISSAO.permite(new ResponsavelPelaCorrecao(UUID.randomUUID(), false), REGISTRO)).isFalse();
    }
}
