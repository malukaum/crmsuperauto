package br.com.superauto.crm.cadastro.repositorio;

import java.util.Objects;
import java.util.UUID;

/** Quem está vendo a ficha, para o histórico dizer o que essa pessoa pode fazer (ATD-04: corrigir). */
public record LeitorDaFicha(UUID id, boolean gerencial) {

    public LeitorDaFicha {
        Objects.requireNonNull(id, "id");
    }
}
