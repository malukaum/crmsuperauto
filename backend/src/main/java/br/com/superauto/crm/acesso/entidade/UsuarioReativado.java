package br.com.superauto.crm.acesso.entidade;

import java.util.UUID;

/** Evento de domínio: o login da pessoa voltou a valer. */
public record UsuarioReativado(UUID usuarioId, boolean estavaAtivo, UUID autor) {
}
