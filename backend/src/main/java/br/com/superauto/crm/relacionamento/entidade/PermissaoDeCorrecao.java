package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * ATD-04 · Quem pode corrigir um atendimento: o autor do registro, até o fim do dia seguinte ao registro (em
 * Brasília), ou o gerencial, a qualquer momento. Usada para gravar a correção e para a ficha mostrar o
 * botão só a quem pode.
 *
 * @param autor quem registrou o atendimento
 * @param registradoEm quando o atendimento foi registrado no sistema
 */
public record PermissaoDeCorrecao(UUID autor, OffsetDateTime registradoEm) {
    private static final int DIAS_PARA_O_AUTOR_CORRIGIR = 1;

    public PermissaoDeCorrecao {
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(registradoEm, "registradoEm");
    }

    /** @param agora o instante atual no fuso de Brasília (RG-01) */
    public boolean permite(ResponsavelPelaCorrecao responsavel, ZonedDateTime agora) {
        return responsavel.gerencial() || (ehAutor(responsavel) && dentroDoPrazo(agora));
    }

    void exigir(ResponsavelPelaCorrecao responsavel, ZonedDateTime agora) {
        if (responsavel.gerencial()) return;
        if (!ehAutor(responsavel))
            throw NegocioException.invalido(
                    "correcao-nao-permitida",
                    "Só o autor do registro ou o gerencial pode corrigir este atendimento.");
        if (!dentroDoPrazo(agora))
            throw NegocioException.invalido(
                    "correcao-fora-do-prazo",
                    "O autor pode corrigir até o fim do dia seguinte ao registro. Peça a correção ao gerencial.");
    }

    private boolean ehAutor(ResponsavelPelaCorrecao responsavel) {
        return autor.equals(responsavel.id());
    }

    private boolean dentroDoPrazo(ZonedDateTime agora) {
        LocalDate ultimoDia =
                registradoEm.atZoneSameInstant(agora.getZone()).toLocalDate().plusDays(DIAS_PARA_O_AUTOR_CORRIGIR);
        return !agora.toLocalDate().isAfter(ultimoDia);
    }
}
