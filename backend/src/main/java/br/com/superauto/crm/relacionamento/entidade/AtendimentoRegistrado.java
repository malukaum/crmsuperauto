package br.com.superauto.crm.relacionamento.entidade;

import java.util.Optional;

/**
 * Contato inserido no histórico, publicado após a atribuição do id.
 *
 * @param corrigeAtendimentoId o atendimento que este registro corrige (ATD-04)
 */
public record AtendimentoRegistrado(Long atendimentoId, Atendimento.Novo dados, Optional<Long> corrigeAtendimentoId) {}
