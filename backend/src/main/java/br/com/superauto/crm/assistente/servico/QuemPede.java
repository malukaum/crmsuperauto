package br.com.superauto.crm.assistente.servico;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Quem usa o assistente: o id e, para o vendedor, o escopo da carteira dele (RG-03). */
public record QuemPede(UUID id, Optional<UUID> vendedor) {

    public QuemPede {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(vendedor, "vendedor");
    }
}
