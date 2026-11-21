package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.TextoPseudonimizado;

/** IA-02 / IA-06 · O que vai para o provedor (já pseudonimizado) para resumir um atendimento. */
public record MaterialDoResumo(Long atendimentoId, TextoPseudonimizado texto) {}
