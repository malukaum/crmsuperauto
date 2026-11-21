package br.com.superauto.crm.relacionamento.entidade;

import java.util.Objects;
import java.util.UUID;

/** ATD-04 · Quem pede a correção: o autor do registro ou o gerencial. */
public record Corretor(UUID id, boolean gerencial) {

    public Corretor {
        Objects.requireNonNull(id, "id");
    }
}
