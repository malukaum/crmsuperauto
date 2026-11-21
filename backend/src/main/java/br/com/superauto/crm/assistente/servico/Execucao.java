package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;

/** O resultado de uma tarefa do assistente e quem o gerou (IA-05: provedor, modelo e se foi o reserva). */
public record Execucao<T>(T resultado, ProvedorDeIa provedor, String modelo, boolean reserva) {}
