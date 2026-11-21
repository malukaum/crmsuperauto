package br.com.superauto.crm.assistente.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

class RotaDaTarefaTest {

    @Test
    @DisplayName("IA-05: a tarefa tenta primeiro o principal e depois o reserva")
    void ordem() {
        var rota = ConfiguracoesDeTeste.rota(TarefaDeIa.RESUMO, ProvedorDeIa.CLAUDE, ProvedorDeIa.GEMINI);

        assertThat(rota.ordemDeTentativa()).containsExactly(ProvedorDeIa.CLAUDE, ProvedorDeIa.GEMINI);
    }

    @Test
    @DisplayName("IA-05: o reserva precisa ser diferente do principal")
    void reservaDiferente() {
        var rota = ConfiguracoesDeTeste.rota(TarefaDeIa.RESUMO, ProvedorDeIa.CLAUDE, null);

        assertThatThrownBy(() -> rota.definir(0, ProvedorDeIa.CHATGPT, Optional.of(ProvedorDeIa.CHATGPT), UUID.randomUUID()))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("reserva-igual");
    }
}
