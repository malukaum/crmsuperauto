package br.com.superauto.crm.cadastro.entidade;

import java.util.List;
import java.util.UUID;

/** Evento de domínio: os contatos do associado mudaram (CAR-04). Leva a lista antes e depois. */
public record ContatosAlterados(Long associadoId, List<ContatoResumo> anteriores, List<ContatoResumo> atuais,
                                UUID autor) {
}
