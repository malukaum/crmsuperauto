package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;

import java.time.LocalDate;

/** Dados do caso de uso de relacionamento. */
public record RegistroDeContato(
        int versao, ResultadoContato resultado, LocalDate retorno, String descricao) {}
