package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * ATD-03 · Uma mensagem de aniversário para a pessoa. Só uma por pessoa no ano: se já houver, a nova é recusada
 * (também garantido pelo índice único {@code uk_atendimento_aniversario_ano}, para registros simultâneos).
 *
 * @param agora quando a mensagem foi registrada, no fuso de Brasília (RG-01); o ano vem daqui
 * @param jaRegistradaNoAno já existe mensagem para a pessoa neste ano
 */
public record MensagemDeAniversario(
        Long associadoId, Short tipoId, OffsetDateTime agora, UUID autor, boolean jaRegistradaNoAno) {

    public MensagemDeAniversario {
        Objects.requireNonNull(associadoId, "associadoId");
        Objects.requireNonNull(tipoId, "tipoId");
        Objects.requireNonNull(agora, "agora");
        Objects.requireNonNull(autor, "autor");
    }

    void exigirPrimeiraDoAno() {
        if (jaRegistradaNoAno)
            throw NegocioException.conflito(
                    "aniversario-ja-registrado",
                    "Já há uma mensagem de aniversário registrada para este cliente neste ano.");
    }

    int ano() {
        return agora.getYear();
    }
}
