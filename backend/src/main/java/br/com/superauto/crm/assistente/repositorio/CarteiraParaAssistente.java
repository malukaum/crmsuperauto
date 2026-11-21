package br.com.superauto.crm.assistente.repositorio;

import br.com.superauto.crm.assistente.entidade.DadosIdentificaveis;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Porta declarada pelo Assistente e implementada pelo Cadastro, dono do associado e dos contratos: o que
 * identifica o cliente (para pseudonimizar, IA-06) e o escopo do vendedor (RG-03).
 */
public interface CarteiraParaAssistente {

    DadosIdentificaveis identificaveis(Long associadoId);

    /** Os associados com algum contrato do vendedor. */
    Set<Long> associadosDoVendedor(UUID vendedor);

    /** O associado existe e, para o vendedor, tem contrato dele. */
    boolean associadoVisivel(Long associadoId, Optional<UUID> vendedor);
}
