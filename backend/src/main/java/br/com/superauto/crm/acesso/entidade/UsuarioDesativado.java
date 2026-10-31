package br.com.superauto.crm.acesso.entidade;

import java.util.UUID;

/**
 * Evento de domínio: o login da pessoa foi desativado (ninguém é excluído, RG-07).
 *
 * @param transferidosPara nulo quando não havia contratos a transferir
 */
public record UsuarioDesativado(UUID usuarioId, boolean estavaAtivo, int contratosTransferidos,
                                UUID transferidosPara, UUID autor) {
}
