package br.com.superauto.crm.cadastro.servico;

/**
 * CAR-02 · A pessoa encontrada pelo documento no início do cadastro: o suficiente para confirmar que é ela e
 * informar só o veículo e o contrato.
 */
public record PessoaCadastrada(Long id, String nome, String cidade, String uf) {
}
