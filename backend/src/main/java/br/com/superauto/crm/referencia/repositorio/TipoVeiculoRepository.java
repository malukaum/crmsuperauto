package br.com.superauto.crm.referencia.repositorio;

import java.util.List;

import br.com.superauto.crm.referencia.entidade.TipoVeiculo;

import org.springframework.data.repository.Repository;

/** Só leitura até existir a manutenção dos tipos de veículo. */
public interface TipoVeiculoRepository extends Repository<TipoVeiculo, Short> {

    List<TipoVeiculo> findByAtivoTrueOrderByDescricao();

    boolean existsByIdAndAtivoTrue(Short id);
}
