package br.com.superauto.crm.compartilhado.entidade;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Persiste o telefone normalizado com o código do país (RG-02). */
@Converter
public class TelefoneConverter implements AttributeConverter<Telefone, String> {
    public String convertToDatabaseColumn(Telefone telefone) {
        return telefone == null ? null : telefone.valor();
    }

    public Telefone convertToEntityAttribute(String valor) {
        return valor == null ? null : new Telefone(valor);
    }
}
