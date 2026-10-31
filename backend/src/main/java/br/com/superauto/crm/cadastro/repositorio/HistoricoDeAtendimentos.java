package br.com.superauto.crm.cadastro.repositorio;

import java.util.List;

/**
 * O histórico de atendimentos de um associado, do mais recente ao mais antigo (CAR-03). Porta declarada pelo
 * Cadastro e implementada pelo Relacionamento, dono dos atendimentos.
 */
public interface HistoricoDeAtendimentos {

    List<AtendimentoDoHistorico> doAssociado(Long associadoId);
}
