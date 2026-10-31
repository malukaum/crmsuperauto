package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;

import java.math.BigDecimal;

/** Resultado da agregação por tipo de veículo e situação, sem carregar contratos individuais. */
public record ContagemDaCarteira(
        String tipoVeiculo, SituacaoNaCarteira situacao, long contratos, BigDecimal mensalidades) {}
