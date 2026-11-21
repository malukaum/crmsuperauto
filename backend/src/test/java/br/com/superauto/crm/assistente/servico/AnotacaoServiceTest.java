package br.com.superauto.crm.assistente.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.assistente.entidade.SituacaoDoResumo;
import br.com.superauto.crm.assistente.repositorio.AnotacaoDoAtendimentoRepository;
import br.com.superauto.crm.assistente.repositorio.AtendimentosParaAssistente;
import br.com.superauto.crm.assistente.repositorio.CarteiraParaAssistente;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

class AnotacaoServiceTest {
    private final AnotacaoDoAtendimentoRepository anotacoes = mock(AnotacaoDoAtendimentoRepository.class);
    private final AtendimentosParaAssistente atendimentos = mock(AtendimentosParaAssistente.class);
    private final AnotacaoService servico = new AnotacaoService(anotacoes, atendimentos, mock(CarteiraParaAssistente.class));
    private final QuemPede vendedor = new QuemPede(UUID.randomUUID(), Optional.of(UUID.randomUUID()));

    @Test
    @DisplayName("IA-01: a anotação é gravada na hora, aguardando o resumo")
    void anota() {
        when(atendimentos.associadoDoAtendimento(10L, vendedor.vendedor())).thenReturn(Optional.of(7L));
        when(anotacoes.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        var anotacao = servico.anotar(new PedidoDeAnotacao(10L, "Cliente quer indicar o irmão", List.of()), vendedor);

        assertThat(anotacao.situacao()).isEqualTo(SituacaoDoResumo.AGUARDANDO);
    }

    @Test
    @DisplayName("IA-01 / RG-03: atendimento fora da carteira do vendedor responde como não encontrado")
    void foraDaCarteira() {
        when(atendimentos.associadoDoAtendimento(10L, vendedor.vendedor())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.anotar(new PedidoDeAnotacao(10L, "texto", List.of()), vendedor))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Atendimento não encontrado.");
        verify(anotacoes, never()).save(any());
    }
}
