package br.com.superauto.crm.acesso.entidade;

import java.util.UUID;

import br.com.superauto.crm.compartilhado.entidade.Email;

/** Evento de domínio: uma pessoa da equipe ganhou conta no CRM com o papel informado. */
public record UsuarioCriado(UUID usuarioId, String username, String nome, Email email, String papel, UUID autor) {
}
