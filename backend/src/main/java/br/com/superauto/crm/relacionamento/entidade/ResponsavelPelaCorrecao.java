package br.com.superauto.crm.relacionamento.entidade;

import java.util.Objects;
import java.util.UUID;

/** ATD-04 · Quem pede a correção de um atendimento: o autor do registro ou o gerencial. */
public record ResponsavelPelaCorrecao(UUID id, boolean gerencial) {

    public ResponsavelPelaCorrecao {
        Objects.requireNonNull(id, "id");
    }
}
