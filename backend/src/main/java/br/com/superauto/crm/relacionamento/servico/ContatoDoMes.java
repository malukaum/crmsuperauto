package br.com.superauto.crm.relacionamento.servico;

import java.time.OffsetDateTime;

/** Dados do caso de uso de relacionamento. */
public record ContatoDoMes(
        Long id,
        Long associadoId,
        String nome,
        String placa,
        String tipo,
        String etapa,
        String resultado,
        String descricao,
        OffsetDateTime realizadoEm,
        String registradoPor) {}
