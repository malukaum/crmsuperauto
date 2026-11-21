package br.com.superauto.crm.assistente.repositorio;

import br.com.superauto.crm.assistente.entidade.RotaDaTarefa;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Repositório do agregado {@link RotaDaTarefa}. */
public interface RotaDaTarefaRepository extends JpaRepository<RotaDaTarefa, TarefaDeIa> {

    /** RG-06 · Carrega travando a linha até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RotaDaTarefa r where r.tarefa = :tarefa")
    Optional<RotaDaTarefa> buscarParaAlteracao(TarefaDeIa tarefa);
}
