package br.com.superauto.crm.relacionamento.entidade;

import java.util.UUID;

/** Alteração da agenda publicada pelo acompanhamento na mesma transação. */
public record AcompanhamentoAtualizado(
        Long contratoId,
        UUID autor,
        EstadoDoAcompanhamento anterior,
        EstadoDoAcompanhamento novo) {}
