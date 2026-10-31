package br.com.superauto.crm.auditoria.entidade;

import java.util.Objects;

/** O que foi auditado: o tipo de entidade (ex.: {@code contrato}) e o id dela, como texto. */
public record ItemAuditado(String entidade, String id) {

    public ItemAuditado {
        Objects.requireNonNull(entidade, "entidade");
        Objects.requireNonNull(id, "id");
    }

    public static ItemAuditado de(String entidade, Object id) {
        return new ItemAuditado(entidade, String.valueOf(Objects.requireNonNull(id, "id")));
    }
}
