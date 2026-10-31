package br.com.superauto.crm.auditoria.servico;

import java.util.HashMap;
import java.util.Map;

/** Monta os mapas de valores da auditoria, que aceitam nulo (ex.: contrato que ainda não tinha vendedor). */
final class Campos {

    private Campos() {
    }

    static Map<String, Object> campo(String nome, Object valor) {
        Map<String, Object> dados = new HashMap<>();
        dados.put(nome, valor == null ? null : String.valueOf(valor));
        return dados;
    }
}
