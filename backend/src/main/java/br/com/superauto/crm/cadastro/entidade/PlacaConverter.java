package br.com.superauto.crm.cadastro.entidade;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Grava a {@link Placa} como texto (maiúsculas, sem hífen). */
@Converter
public class PlacaConverter implements AttributeConverter<Placa, String> {

    @Override
    public String convertToDatabaseColumn(Placa placa) {
        return placa == null ? null : placa.valor();
    }

    @Override
    public Placa convertToEntityAttribute(String texto) {
        return texto == null ? null : new Placa(texto);
    }
}
