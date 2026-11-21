package br.com.superauto.crm.assistente.entidade;

import java.util.Map;

/**
 * IA-06 · Texto pronto para ir ao provedor, sem dados do cliente, e o mapa para restaurar a resposta no nosso
 * servidor (marcador → valor original). O mapa nunca sai do servidor.
 */
public record TextoPseudonimizado(String texto, Map<String, String> marcadores) {

    public TextoPseudonimizado {
        marcadores = Map.copyOf(marcadores);
    }
}
