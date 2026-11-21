package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;

import org.springframework.data.repository.Repository;

import java.util.List;

/** Só leitura: o histórico vem de uma visão. */
public interface HistoricoAtendimentoRepository
        extends Repository<LinhaHistoricoAtendimento, Long> {

    List<LinhaHistoricoAtendimento> findByAssociadoIdOrderByRealizadoEmDescIdDesc(Long associadoId);
}
