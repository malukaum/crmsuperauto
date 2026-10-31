package br.com.superauto.crm.relacionamento.repositorio;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Intervalo fechado no início e aberto no fim, com escopo de leitura (RG-01 e RG-03). */
public record PeriodoDeAtendimentos(
        OffsetDateTime inicio, OffsetDateTime fim, Optional<UUID> vendedor) {
    public UUID vendedorId() {
        return vendedor.orElse(null);
    }
}
