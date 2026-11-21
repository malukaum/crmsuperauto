package br.com.superauto.crm.cadastro.repositorio;

import java.util.List;

/**
 * A régua de pós-venda de um contrato como a ficha mostra (CAR-03), com a versão do acompanhamento para registrar o
 * atendimento ou ajustar a régua sem sobrescrever outro registro (RG-06).
 */
public record ReguaDoContrato(int versao, List<EtapaPrevista> etapas) {

    public ReguaDoContrato {
        etapas = List.copyOf(etapas);
    }
}
