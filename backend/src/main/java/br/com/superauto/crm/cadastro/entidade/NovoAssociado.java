package br.com.superauto.crm.cadastro.entidade;

import java.util.Objects;
import java.util.Optional;

import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.compartilhado.entidade.Telefone;

/**
 * O que é preciso para cadastrar uma pessoa (CAR-02): documento, dados pessoais e o WhatsApp, que é
 * obrigatório. O e-mail é opcional.
 */
public record NovoAssociado(Documento documento, DadosPessoais dados, Telefone whatsapp, Optional<Email> email) {

    public NovoAssociado {
        Objects.requireNonNull(documento, "documento");
        Objects.requireNonNull(dados, "dados");
        Objects.requireNonNull(whatsapp, "whatsapp");
        Objects.requireNonNull(email, "email");
    }
}
