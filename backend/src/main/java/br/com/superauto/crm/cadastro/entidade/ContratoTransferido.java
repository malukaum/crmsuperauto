package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/**
 * Evento de domínio: o contrato passou para outro vendedor.
 *
 * @param vendedorAnterior nulo quando o contrato ainda não tinha vendedor
 */
public record ContratoTransferido(Long contratoId, UUID vendedorAnterior, UUID novoVendedor, UUID autor) {
}
