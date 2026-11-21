package br.com.superauto.crm.assistente.repositorio;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta declarada pelo Assistente e implementada pelo Relacionamento, dono dos atendimentos: de quem é o
 * atendimento, respeitando o escopo do vendedor (RG-03).
 */
public interface AtendimentosParaAssistente {

    /** O associado do atendimento, se ele existe e está na carteira de quem pede. */
    Optional<Long> associadoDoAtendimento(Long atendimentoId, Optional<UUID> vendedor);
}
