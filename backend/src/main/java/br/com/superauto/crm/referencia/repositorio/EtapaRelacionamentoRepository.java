package br.com.superauto.crm.referencia.repositorio;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import br.com.superauto.crm.referencia.entidade.EtapaRelacionamento;

import org.springframework.data.repository.Repository;

import java.util.List;

/** Etapas da régua; a gravação publica os eventos da alteração de prazo (FIL-06). */
public interface EtapaRelacionamentoRepository extends Repository<EtapaRelacionamento, Short> {

    List<EtapaRelacionamento> findAllByOrderByOrdem();

    /** RG-06 · As etapas travadas até o fim da transação, para alterar os prazos (FIL-06). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EtapaRelacionamento e order by e.id")
    List<EtapaRelacionamento> buscarTodasParaAlteracao();

    <S extends EtapaRelacionamento> Iterable<S> saveAll(Iterable<S> etapas);
}
