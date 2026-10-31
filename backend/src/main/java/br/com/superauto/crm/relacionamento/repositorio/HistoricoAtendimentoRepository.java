package br.com.superauto.crm.relacionamento.repositorio;

import java.util.List;

import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;

import org.springframework.data.repository.Repository;

/** Só leitura: o histórico vem de uma visão. */
public interface HistoricoAtendimentoRepository extends Repository<LinhaHistoricoAtendimento, Long> {

    List<LinhaHistoricoAtendimento> findByAssociadoIdOrderByRealizadoEmDescIdDesc(Long associadoId);
}
