package br.com.superauto.crm.acesso.servico;

/** Comando: dados para criar uma pessoa da equipe. */
public record NovoUsuario(String username, String nome, String sobrenome, String email, String papel) {
}
