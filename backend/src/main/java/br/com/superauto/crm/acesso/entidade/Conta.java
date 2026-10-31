package br.com.superauto.crm.acesso.entidade;

import java.util.Objects;
import java.util.UUID;

import br.com.superauto.crm.compartilhado.entidade.Email;

/**
 * Conta de login de uma pessoa, como o CRM a enxerga, independente do provedor de identidade.
 *
 * @param email nulo quando a conta não tem e-mail cadastrado
 * @param aguardandoSenha a pessoa ainda não definiu a senha pelo e-mail de convite
 */
public record Conta(UUID id, String username, String nome, String sobrenome, Email email,
                    boolean ativo, boolean aguardandoSenha) {

    /** Nome e sobrenome; sem nenhum dos dois, o username. */
    public String nomeCompleto() {
        String completo = (Objects.requireNonNullElse(nome, "") + " " + Objects.requireNonNullElse(sobrenome, "")).trim();
        return completo.isEmpty() ? username : completo;
    }

    public Conta comAtivo(boolean novoAtivo) {
        return new Conta(id, username, nome, sobrenome, email, novoAtivo, aguardandoSenha);
    }
}
