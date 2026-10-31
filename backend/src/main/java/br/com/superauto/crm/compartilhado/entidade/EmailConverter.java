package br.com.superauto.crm.compartilhado.entidade;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Grava o {@link Email} como texto. */
@Converter
public class EmailConverter implements AttributeConverter<Email, String> {

    @Override
    public String convertToDatabaseColumn(Email email) {
        return email == null ? null : email.valor();
    }

    @Override
    public Email convertToEntityAttribute(String texto) {
        return texto == null ? null : new Email(texto);
    }
}
