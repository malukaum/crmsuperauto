package br.com.superauto.crm.acesso.servico;

import java.util.Objects;

import br.com.superauto.crm.compartilhado.entidade.Email;

/**
 * Dados de uma pessoa da equipe guardados na projeção local. Sem nome, usa o username, porque a coluna
 * {@code nome} é obrigatória.
 *
 * @param email nulo quando a conta não tem e-mail
 */
public record DadosProjecao(String username, String nome, Email email) {

    public DadosProjecao {
        String nomeLimpo = Objects.requireNonNullElse(nome, "").trim();
        nome = nomeLimpo.isEmpty() ? username : nomeLimpo;
    }
}
