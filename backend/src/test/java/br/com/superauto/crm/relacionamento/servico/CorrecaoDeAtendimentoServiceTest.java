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
import br.com.superauto.crm.relacionamento.entidade.ResponsavelPelaCorrecao;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

class CorrecaoDeAtendimentoServiceTest {
    private static final Correcao CORRECAO = new Correcao(ResultadoContato.SEM_RESPOSTA, Optional.empty(), null);
    private static final ResponsavelPelaCorrecao GERENCIAL = new ResponsavelPelaCorrecao(UUID.randomUUID(), true);

    private final AtendimentoRepository atendimentos = mock(AtendimentoRepository.class);
    private final CorrecaoDeAtendimentoService servico =
            new CorrecaoDeAtendimentoService(atendimentos, Clock.systemDefaultZone());

    @Test
    @DisplayName("ATD-04: atendimento já corrigido não recebe outra correção; corrige-se a correção")
    void jaCorrigido() {
        var original = Atendimento.registrar(new Atendimento.Novo(7L, null, (short) 3, null, ResultadoContato.CONCLUIDO,
                null, null, OffsetDateTime.now(), UUID.randomUUID()));
        when(atendimentos.buscarParaAlteracao(10L)).thenReturn(Optional.of(original));
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
        when(atendimentos.buscarParaAlteracao(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.corrigir(10L, CORRECAO, GERENCIAL))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Atendimento não encontrado.");
    }
}
