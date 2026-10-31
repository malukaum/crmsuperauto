package br.com.superauto.crm.relacionamento.repositorio;

import java.util.List;

import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;

/**
 * As etapas da régua de pós-venda, na ordem em que acontecem. Porta declarada pelo Relacionamento e
 * implementada pelo contexto Referência, onde a régua é configurada.
 */
public interface ReguaDePosVenda {

    List<EtapaRegua> etapas();
}
