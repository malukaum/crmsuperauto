package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.NovoAnexo;

import java.util.List;
import java.util.Objects;

/** IA-01 · O que chega da tela para anotar um atendimento. */
public record PedidoDeAnotacao(Long atendimentoId, String texto, List<NovoAnexo> anexos) {

    public PedidoDeAnotacao {
        Objects.requireNonNull(atendimentoId, "atendimentoId");
        anexos = anexos == null ? List.of() : List.copyOf(anexos);
    }
}
