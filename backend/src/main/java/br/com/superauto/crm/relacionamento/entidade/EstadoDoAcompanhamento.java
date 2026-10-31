package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.util.Optional;

/** Fotografia imutável da agenda, anterior ou posterior à alteração (RG-08). */
public record EstadoDoAcompanhamento(
        Optional<Short> etapa,
        Optional<LocalDate> proximoContato,
        Optional<LocalDate> retornoManual) {}
