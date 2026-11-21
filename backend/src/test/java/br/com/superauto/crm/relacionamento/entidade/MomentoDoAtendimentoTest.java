package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

class MomentoDoAtendimentoTest {
    private static final ZonedDateTime AGORA =
            LocalDateTime.of(2026, 10, 31, 12, 0).atZone(ZoneId.of("America/Sao_Paulo"));

    @Test
    @DisplayName("ATD-01: sem data informada, o atendimento é de agora")
    void semDataEhAgora() {
        assertThat(MomentoDoAtendimento.de(Optional.empty(), AGORA).valor()).isEqualTo(AGORA.toOffsetDateTime());
    }

    @Test
    @DisplayName("ATD-01: aceita um atendimento de até 7 dias atrás, no fuso de São Paulo")
    void aceitaSeteDiasAtras() {
        var momento = MomentoDoAtendimento.de(Optional.of(LocalDateTime.of(2026, 10, 24, 12, 0)), AGORA);

        assertThat(momento.data()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(momento.valor().getOffset()).isEqualTo(AGORA.getOffset());
    }

    @Test
    @DisplayName("ATD-01: recusa atendimento com mais de 7 dias ou no futuro")
    void recusaForaDaJanela() {
        assertThatThrownBy(() -> MomentoDoAtendimento.de(Optional.of(LocalDateTime.of(2026, 10, 24, 11, 59)), AGORA))
                .isInstanceOf(NegocioException.class)
                .hasMessage("O atendimento pode ser lançado com até 7 dias de atraso.");
        assertThatThrownBy(() -> MomentoDoAtendimento.de(Optional.of(LocalDateTime.of(2026, 10, 31, 12, 1)), AGORA))
                .isInstanceOf(NegocioException.class)
                .hasMessage("A data e hora do atendimento não pode ser no futuro.");
    }
}
