package br.com.superauto.crm.comercial.repositorio;

import java.util.List;

import br.com.superauto.crm.comercial.entidade.IndicacaoResumida;

import org.springframework.data.repository.Repository;

/** Só leitura. */
public interface IndicacaoResumidaRepository extends Repository<IndicacaoResumida, Long> {

    List<IndicacaoResumida> findByIndicadorIdOrderByRegistradaEmDesc(Long indicadorId);
}
