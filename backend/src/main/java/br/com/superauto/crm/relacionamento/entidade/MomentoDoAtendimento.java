package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * ATD-01 · Quando o atendimento aconteceu: agora ou uma data e hora anterior informada pelo usuário, de até 7 dias
 * atrás, no fuso de São Paulo (RG-01).
 */
public record MomentoDoAtendimento(OffsetDateTime valor) {
    static final int DIAS_DE_ATRASO_PERMITIDOS = 7;

    public MomentoDoAtendimento {
        Objects.requireNonNull(valor, "valor");
    }

    /** @param agora o instante atual no fuso de São Paulo */
    public static MomentoDoAtendimento de(Optional<LocalDateTime> informado, ZonedDateTime agora) {
        if (informado.isEmpty()) {
            return new MomentoDoAtendimento(agora.toOffsetDateTime());
        }
        ZonedDateTime momento = informado.get().atZone(agora.getZone());
        if (momento.isAfter(agora)) {
            throw NegocioException.invalido(
                    "atendimento-no-futuro", "A data e hora do atendimento não pode ser no futuro.");
        }
        if (momento.isBefore(agora.minusDays(DIAS_DE_ATRASO_PERMITIDOS))) {
            throw NegocioException.invalido(
                    "atendimento-antigo", "O atendimento pode ser lançado com até 7 dias de atraso.");
        }
        return new MomentoDoAtendimento(momento.toOffsetDateTime());
    }

    /** O dia do atendimento em São Paulo. */
    public LocalDate data() {
        return valor.toLocalDate();
    }
}
