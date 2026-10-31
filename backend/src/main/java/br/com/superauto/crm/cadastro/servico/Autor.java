package br.com.superauto.crm.cadastro.servico;

import java.util.Objects;
import java.util.UUID;

/** Quem faz a alteração e quanto da carteira essa pessoa enxerga (RG-03). */
public record Autor(UUID id, EscopoCarteira escopo) {

    public Autor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(escopo, "escopo");
    }
}
