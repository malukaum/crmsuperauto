package br.com.superauto.crm.relacionamento.repositorio;

import java.util.Map;
import java.util.UUID;

/** Escrita dos prazos na referência, sem permitir criar ou reordenar etapas. */
public interface ConfiguracaoDosPrazos {
    void alterarPrazos(Map<Short, Integer> prazos, UUID autor);
}
