package br.com.superauto.crm.cadastro.repositorio;

import java.time.OffsetDateTime;

/** Uma indicação feita pelo associado, como a ficha mostra (CAR-03). */
public record IndicacaoFeita(Long id, String indicadoNome, String status, OffsetDateTime registradaEm) {
}
