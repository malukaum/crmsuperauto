package br.com.superauto.crm.referencia.repositorio;

import java.util.List;

import br.com.superauto.crm.referencia.entidade.EtapaRelacionamento;

import org.springframework.data.repository.Repository;

/** Só leitura até a configuração dos prazos da régua (FIL-06). */
public interface EtapaRelacionamentoRepository extends Repository<EtapaRelacionamento, Short> {

    List<EtapaRelacionamento> findAllByOrderByOrdem();
}
