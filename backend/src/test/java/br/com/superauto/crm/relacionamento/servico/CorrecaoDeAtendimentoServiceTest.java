package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.Correcao;
import br.com.superauto.crm.relacionamento.entidade.Corretor;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

class CorrecaoDeAtendimentoServiceTest {
    private static final Correcao CORRECAO = new Correcao(ResultadoContato.SEM_RESPOSTA, Optional.empty(), null);
    private static final Corretor GERENCIAL = new Corretor(UUID.randomUUID(), true);

    private final AtendimentoRepository atendimentos = mock(AtendimentoRepository.class);
    private final CorrecaoDeAtendimentoService servico =
            new CorrecaoDeAtendimentoService(atendimentos, Clock.systemDefaultZone());

    @Test
    @DisplayName("ATD-04: atendimento já corrigido não recebe outra correção; corrige-se a correção")
    void jaCorrigido() {
        when(atendimentos.findById(10L)).thenReturn(Optional.of(mock(Atendimento.class)));
        when(atendimentos.existsByCorrigeAtendimentoId(10L)).thenReturn(true);

        assertThatThrownBy(() -> servico.corrigir(10L, CORRECAO, GERENCIAL))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("atendimento-ja-corrigido");
        verify(atendimentos, never()).save(any());
    }

    @Test
    @DisplayName("ATD-04: atendimento inexistente responde como não encontrado")
    void naoEncontrado() {
        when(atendimentos.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.corrigir(10L, CORRECAO, GERENCIAL))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Atendimento não encontrado.");
    }
}
