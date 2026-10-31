package br.com.superauto.crm.cadastro.entidade;

import java.math.BigDecimal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
