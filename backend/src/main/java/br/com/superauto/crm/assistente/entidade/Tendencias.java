package br.com.superauto.crm.assistente.entidade;

import java.util.Map;

/**
 * IA-03 · Indicadores do período, calculados a partir dos resumos (nunca pela IA).
 *
 * @param porAssunto quantos atendimentos citaram cada assunto, do mais comum para o menos comum
 */
public record Tendencias(long atendimentosResumidos, Map<Assunto, Long> porAssunto, Map<Satisfacao, Long> porSatisfacao,
                         long comRiscoDeCancelamento, long comChanceAltaDeIndicar) {

    public Tendencias {
        porAssunto = Map.copyOf(porAssunto);
        porSatisfacao = Map.copyOf(porSatisfacao);
    }
}
