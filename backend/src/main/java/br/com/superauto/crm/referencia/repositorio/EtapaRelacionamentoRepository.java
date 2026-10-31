package br.com.superauto.crm.referencia.repositorio;

import br.com.superauto.crm.referencia.entidade.EtapaRelacionamento;

import org.springframework.data.repository.Repository;

import java.util.List;

/** Etapas da régua; a gravação publica os eventos da alteração de prazo (FIL-06). */
public interface EtapaRelacionamentoRepository extends Repository<EtapaRelacionamento, Short> {

    List<EtapaRelacionamento> findAllByOrderByOrdem();

    <S extends EtapaRelacionamento> Iterable<S> saveAll(Iterable<S> etapas);
}
