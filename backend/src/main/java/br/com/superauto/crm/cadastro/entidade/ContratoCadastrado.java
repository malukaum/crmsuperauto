package br.com.superauto.crm.cadastro.entidade;

import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento de domínio: um contrato novo foi cadastrado na tela (CAR-02). O Relacionamento reage colocando o
 * contrato na régua; a auditoria registra a criação.
 *
 * @param dataAdesao dia da adesão (cadastro no SGA) em São Paulo, base do primeiro contato da régua
 */
public record ContratoCadastrado(Long contratoId, Long associadoId, Long veiculoId, UUID vendedorId,
                                 Mensalidade mensalidade, LocalDate dataAdesao, UUID autor) {
}
