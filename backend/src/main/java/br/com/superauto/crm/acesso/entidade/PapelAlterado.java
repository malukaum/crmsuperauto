package br.com.superauto.crm.acesso.entidade;

import java.util.UUID;

/** Evento de domínio: a pessoa da equipe passou a ter outro papel. */
public record PapelAlterado(UUID usuarioId, String papelAnterior, String papelNovo, UUID autor) {
}
