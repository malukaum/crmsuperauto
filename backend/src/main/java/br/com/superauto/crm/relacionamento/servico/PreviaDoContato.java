package br.com.superauto.crm.relacionamento.servico;

import java.time.LocalDate;

/** Dados do caso de uso de relacionamento. */
public record PreviaDoContato(
        String etapaConcluida, String proximaEtapa, LocalDate proximoContato) {}
