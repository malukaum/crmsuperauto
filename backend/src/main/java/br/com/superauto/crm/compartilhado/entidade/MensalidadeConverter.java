package br.com.superauto.crm.compartilhado.entidade;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/** Grava a {@link Mensalidade} em {@code numeric(12,2)}. */
@Converter
public class MensalidadeConverter implements AttributeConverter<Mensalidade, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Mensalidade mensalidade) {
        return mensalidade == null ? null : mensalidade.valor();
    }

    @Override
    public Mensalidade convertToEntityAttribute(BigDecimal valor) {
        return valor == null ? null : new Mensalidade(valor);
    }
}
