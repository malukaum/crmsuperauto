package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repositório do agregado {@link AcompanhamentoContrato}. */
public interface AcompanhamentoContratoRepository extends JpaRepository<AcompanhamentoContrato, Long> {

    /**
     * RG-06 · Carrega o acompanhamento travando a linha até o fim da transação: registros simultâneos do mesmo
     * contrato entram em fila, e cada um parte do estado deixado pelo anterior.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AcompanhamentoContrato a where a.contratoId = :contratoId")
    Optional<AcompanhamentoContrato> buscarParaAlteracao(Long contratoId);

    /** RG-06 · Carrega travando as linhas, sempre na ordem do id (evita deadlock), até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AcompanhamentoContrato a where a.contratoId in :contratoIds order by a.contratoId")
    List<AcompanhamentoContrato> buscarTodosParaAlteracao(Collection<Long> contratoIds);
}
