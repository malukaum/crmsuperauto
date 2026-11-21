package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class MensagemDeAniversarioTest {
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-31T12:00:00-03:00");

    private static MensagemDeAniversario mensagem(boolean jaRegistradaNoAno) {
        return new MensagemDeAniversario(7L, (short) 2, AGORA, UUID.randomUUID(), jaRegistradaNoAno);
    }

    @Test
    @DisplayName("ATD-03: já havendo mensagem para a pessoa no ano, a nova é recusada")
    void segundaRecusada() {
        assertThatThrownBy(() -> Atendimento.registrarMensagemDeAniversario(mensagem(true)))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("aniversario-ja-registrado");
    }

    @Test
    @DisplayName("ATD-03: a primeira mensagem do ano é gravada como Aniversário, ligada à pessoa e sem contrato")
    void primeiraGravada() {
        var atendimento = Atendimento.registrarMensagemDeAniversario(mensagem(false));

        assertThat(atendimento.getAssociadoId()).isEqualTo(7L);
        assertThat(atendimento.getContratoId()).isEmpty();
    }
}
