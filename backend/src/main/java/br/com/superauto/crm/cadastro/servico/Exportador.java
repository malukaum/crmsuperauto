package br.com.superauto.crm.cadastro.servico;

import java.util.Objects;
import java.util.UUID;

/**
 * Quem exporta a carteira (CAR-07) e o que pode levar: o vendedor, só os seus contratos; o documento sai
 * completo só para o gerencial (proposta).
 */
public record Exportador(UUID id, EscopoCarteira escopo, boolean gerencial) {

    public Exportador {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(escopo, "escopo");
    }

    public boolean veDocumentoCompleto() {
        return gerencial;
    }
}
