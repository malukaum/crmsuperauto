package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.TarefaDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.PedidoDaIa;
import br.com.superauto.crm.erro.NegocioException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * IA-01 / IA-02 · Em segundo plano, resume as anotações que estão aguardando: prepara o material pseudonimizado,
 * chama o provedor da tarefa (com reserva) e guarda o resumo. Falha conta tentativa; sem provedor, só espera.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessamentoDeAnotacoes {
    private final EtapasDoResumo etapas;
    private final ExecutorDeIa executor;
    private final InterpretacaoDasRespostas interpretacao;

    @Scheduled(fixedDelayString = "${crm.ia.intervalo-do-processamento:PT20S}", initialDelayString = "PT30S")
    public void processarPendentes() {
        etapas.pendentes().forEach(this::processar);
    }

    void processar(Long atendimentoId) {
        try {
            etapas.preparar(atendimentoId).ifPresent(material -> {
                var pedido = new PedidoDaIa(InstrucoesDoAssistente.RESUMO, material.texto().texto(),
                        InstrucoesDoAssistente.TOKENS_DO_RESUMO);
                var execucao = executor.executar(TarefaDeIa.RESUMO, pedido, Optional.of(atendimentoId),
                        resposta -> interpretacao.resumo(resposta, material.texto().marcadores()));
                etapas.concluir(atendimentoId, execucao);
            });
        } catch (NegocioException e) {
            if (ExecutorDeIa.CODIGO_SEM_PROVEDOR.equals(e.getCodigo())) etapas.adiar(atendimentoId);
            else falhar(atendimentoId, e);
        } catch (RuntimeException e) {
            falhar(atendimentoId, e);
        }
    }

    private void falhar(Long atendimentoId, RuntimeException e) {
        log.warn("Resumo do atendimento {} não saiu: {}", atendimentoId, e.getMessage());
        etapas.falhar(atendimentoId, e.getMessage());
    }
}
