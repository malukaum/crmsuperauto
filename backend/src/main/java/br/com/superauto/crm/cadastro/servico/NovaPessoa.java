package br.com.superauto.crm.cadastro.servico;

import java.util.Optional;

import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.compartilhado.entidade.Telefone;

/** Os dados de uma pessoa que ainda não está na carteira (CAR-02). */
public record NovaPessoa(DadosPessoais dados, Telefone whatsapp, Optional<Email> email) {
}
