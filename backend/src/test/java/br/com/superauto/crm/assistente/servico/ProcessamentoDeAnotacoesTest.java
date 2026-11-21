package br.com.superauto.crm.assistente.servico;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.assistente.entidade.TextoPseudonimizado;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

class ProcessamentoDeAnotacoesTest {
    private final EtapasDoResumo etapas = mock(EtapasDoResumo.class);
    private final ExecutorDeIa executor = mock(ExecutorDeIa.class);
    private final ProcessamentoDeAnotacoes processamento =
            new ProcessamentoDeAnotacoes(etapas, executor, new InterpretacaoDasRespostas(new Pseudonimizacao()));

    @BeforeEach
    void preparar() {
        when(etapas.preparar(10L)).thenReturn(Optional.of(new MaterialDoResumo(10L, new TextoPseudonimizado("texto", Map.of()))));
    }

    @Test
    @DisplayName("IA-01: com o assistente desligado, a anotação só espera, sem gastar tentativa")
    void semProvedorAdia() {
        when(executor.executar(any(), any(), any(), any()))
                .thenThrow(NegocioException.conflito(ExecutorDeIa.CODIGO_SEM_PROVEDOR, "desligado"));

        processamento.processar(10L);

        verify(etapas).adiar(10L);
        verify(etapas, never()).falhar(any(), anyString());
    }

    @Test
    @DisplayName("IA-01: com todos os provedores falhando, conta uma tentativa para tentar de novo depois")
    void falhaContaTentativa() {
        when(executor.executar(any(), any(), any(), any())).thenThrow(new IllegalStateException("fora do ar"));

        processamento.processar(10L);

        verify(etapas).falhar(eq(10L), anyString());
    }
}
