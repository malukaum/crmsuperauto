package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/** Evento de domínio: nome, nascimento ou cidade do associado mudaram. */
public record DadosPessoaisCorrigidos(Long associadoId, DadosPessoais anteriores, DadosPessoais novos, UUID autor) {
}
