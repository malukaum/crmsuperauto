package br.com.superauto.crm.cadastro.servico;

import br.com.superauto.crm.cadastro.entidade.DadosContato;

/** CAR-04 · Um contato existente com os novos dados. */
public record AlteracaoContato(Long contatoId, DadosContato dados) {
}
