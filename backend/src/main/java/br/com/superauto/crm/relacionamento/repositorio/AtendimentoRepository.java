package br.com.superauto.crm.relacionamento.repositorio;

import java.util.List;

import br.com.superauto.crm.relacionamento.entidade.Atendimento;

import org.springframework.data.repository.Repository;

/** Repositório do agregado {@link Atendimento}. Sem {@code delete}: atendimento nunca é excluído (RG-07). */
public interface AtendimentoRepository extends Repository<Atendimento, Long> {

    List<Atendimento> findByAssociadoId(Long associadoId);
}
