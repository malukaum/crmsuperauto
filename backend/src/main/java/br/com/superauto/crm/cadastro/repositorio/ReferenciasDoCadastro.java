package br.com.superauto.crm.cadastro.repositorio;

import java.util.Optional;

/**
 * O que o cadastro precisa das tabelas de referência (municípios do IBGE e tipos de veículo). Porta declarada
 * pelo Cadastro e implementada pelo contexto Referência, dono desses dados.
 */
public interface ReferenciasDoCadastro {

    Optional<Localidade> municipio(Integer municipioId);

    /** Só tipos ativos são aceitos em veículo novo ou alterado. */
    boolean tipoVeiculoAtivo(Short tipoVeiculoId);
}
