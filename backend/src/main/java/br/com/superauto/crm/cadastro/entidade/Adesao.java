package br.com.superauto.crm.cadastro.entidade;

import java.time.OffsetDateTime;
import java.util.Objects;

/** A adesão de um associado à proteção de um veículo: o que é preciso para nascer um contrato. */
public record Adesao(Long associadoId, Long veiculoId, Mensalidade mensalidade, OffsetDateTime aderidoEm) {

    public Adesao {
        Objects.requireNonNull(associadoId, "associadoId");
        Objects.requireNonNull(veiculoId, "veiculoId");
        Objects.requireNonNull(mensalidade, "mensalidade");
        Objects.requireNonNull(aderidoEm, "aderidoEm");
    }
}
