package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do agregado {@link AcompanhamentoContrato}. */
public interface AcompanhamentoContratoRepository extends JpaRepository<AcompanhamentoContrato, Long> {
}
