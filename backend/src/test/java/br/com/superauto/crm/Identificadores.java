package br.com.superauto.crm;

import java.lang.reflect.Field;

/**
 * Só para testes: atribui o id que o banco geraria (identity), para exercitar regras que dependem dele sem subir o
 * Hibernate.
 */
public final class Identificadores {

    private Identificadores() {
    }

    public static <T> T comId(T entidade, Object id) {
        try {
            Field campo = entidade.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidade, id);
            return entidade;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Entidade sem campo id: " + entidade.getClass(), e);
        }
    }
}
