package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/**
 * Evento de domínio: o código do contrato no SGA mudou (CAR-05).
 *
 * @param anterior nulo quando ainda não havia código
 * @param novo nulo quando o código foi retirado
 */
public record CodigoSgaAlterado(Long contratoId, String anterior, String novo, UUID autor) {
}
