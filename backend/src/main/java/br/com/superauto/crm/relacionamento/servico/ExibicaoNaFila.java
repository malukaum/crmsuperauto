package br.com.superauto.crm.relacionamento.servico;

import java.time.LocalDate;

/** Calendário e estado da mensagem usados na apresentação da fila. */
public record ExibicaoNaFila(LocalDate data, LocalDate hoje, boolean enviada) {}
