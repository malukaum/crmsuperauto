package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * FIL-02 e FIL-03 · Qual fila de contatos consultar.
 *
 * @param atrasados verdadeiro para Atrasados; falso para Contatos de hoje
 * @param vendedor preenchido só quando quem pede é vendedor (RG-03)
 */
public record FilaSolicitada(boolean atrasados, FiltroDaFila filtro, Optional<UUID> vendedor) {

    public FilaSolicitada {
        Objects.requireNonNull(filtro, "filtro");
        Objects.requireNonNull(vendedor, "vendedor");
    }
}
