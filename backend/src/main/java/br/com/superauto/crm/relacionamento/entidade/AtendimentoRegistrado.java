package br.com.superauto.crm.relacionamento.entidade;

/** Contato inserido no histórico, publicado após a atribuição do id. */
public record AtendimentoRegistrado(Long atendimentoId, Atendimento.Novo dados) {}
