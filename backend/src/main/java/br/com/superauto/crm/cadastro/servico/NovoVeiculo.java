package br.com.superauto.crm.cadastro.servico;

import br.com.superauto.crm.cadastro.entidade.Placa;

/** O veículo do novo contrato (CAR-02). */
public record NovoVeiculo(Placa placa, String modelo, Short tipoVeiculoId) {
}
