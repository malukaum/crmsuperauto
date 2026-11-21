package br.com.superauto.crm.assistente.servico;

/** IA-05 · O resultado do "Testar conexão" de um provedor. */
public record ResultadoDoTeste(boolean conectou, String mensagem) {}
