package br.com.superauto.crm.assistente.entidade;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Evento de domínio: a configuração do assistente de IA mudou (IA-05), para a auditoria. Nunca leva a chave. */
public record ConfiguracaoDeIaAlterada(String item, String id, Map<String, Object> anterior, Map<String, Object> novo,
                                       UUID autor) {

    public ConfiguracaoDeIaAlterada {
        anterior = imutavel(anterior);
        novo = imutavel(novo);
    }

    /** Aceita valor vazio (ex.: o modelo antes da primeira configuração), que {@code Map.copyOf} recusaria. */
    private static Map<String, Object> imutavel(Map<String, Object> valores) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(valores));
    }
}
