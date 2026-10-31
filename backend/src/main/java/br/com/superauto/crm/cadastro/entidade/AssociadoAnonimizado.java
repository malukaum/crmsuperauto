package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/**
 * Evento de domínio: os dados pessoais do associado foram apagados a pedido do titular (CAR-08). Não carrega
 * nenhum dado pessoal.
 *
 * @param usuarioId conta da área do cliente a desativar; nulo se o associado nunca fez o primeiro acesso
 */
public record AssociadoAnonimizado(Long associadoId, UUID usuarioId, UUID autor) {
}
