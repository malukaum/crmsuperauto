package br.com.superauto.crm.assistente.entidade;

import java.util.List;

/** IA-03 · O que um resumo traz para os indicadores, sem nada que identifique o cliente. */
public record SinalDoAtendimento(List<String> assuntos, Satisfacao satisfacao, Boolean riscoDeCancelamento,
                                 ChanceDeIndicar chanceDeIndicar) {

    public SinalDoAtendimento {
        assuntos = assuntos == null ? List.of() : List.copyOf(assuntos);
    }
}
