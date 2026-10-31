package br.com.superauto.crm.cadastro.entidade;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.compartilhado.entidade.Telefone;

/**
 * Por onde falar com o associado: o tipo e o valor já normalizado (telefone como 55 + DDD + número, e-mail em
 * minúsculas). Objeto de valor: se valida na criação conforme o tipo, como o {@code ck_contato_associado_valor}.
 */
@Embeddable
public record MeioContato(
        @Enumerated(EnumType.STRING) @Column(name = "tipo", nullable = false, length = 10) TipoContatoAssociado tipo,
        @Column(name = "valor", nullable = false, length = Email.TAMANHO_MAXIMO) String valor) {

    public MeioContato {
        Objects.requireNonNull(tipo, "tipo");
        valor = tipo.telefonico() ? new Telefone(valor).valor() : new Email(valor).valor();
    }

    /** Valor digitado na tela: o telefone aceita com ou sem 55, parênteses, espaços e traços (CAR-04). */
    public static MeioContato de(TipoContatoAssociado tipo, String digitado) {
        String normalizado = tipo.telefonico() ? Telefone.de(digitado).valor() : Email.de(digitado).valor();
        return new MeioContato(tipo, normalizado);
    }

    public static MeioContato whatsapp(Telefone telefone) {
        return new MeioContato(TipoContatoAssociado.WHATSAPP, telefone.valor());
    }

    public static MeioContato email(Email email) {
        return new MeioContato(TipoContatoAssociado.EMAIL, email.valor());
    }

    public Optional<Telefone> telefone() {
        return tipo.telefonico() ? Optional.of(new Telefone(valor)) : Optional.empty();
    }

    /** Como mostrar na tela e na exportação (RG-02). */
    public String exibicao() {
        return telefone().map(Telefone::formatado).orElse(valor);
    }
}
