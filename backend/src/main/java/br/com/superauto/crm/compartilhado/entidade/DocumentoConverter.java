package br.com.superauto.crm.compartilhado.entidade;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Grava o {@link Documento} como texto sem pontuação; vazio só em cadastro anonimizado. */
@Converter
public class DocumentoConverter implements AttributeConverter<Documento, String> {

    @Override
    public String convertToDatabaseColumn(Documento documento) {
        return documento == null ? null : documento.valor();
    }

    @Override
    public Documento convertToEntityAttribute(String texto) {
        return texto == null ? null : new Documento(texto);
    }
}
