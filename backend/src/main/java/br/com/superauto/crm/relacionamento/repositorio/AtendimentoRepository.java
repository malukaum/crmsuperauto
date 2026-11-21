package br.com.superauto.crm.relacionamento.repositorio;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Histórico preservado: o repositório do agregado não expõe exclusão (RG-07). */
public interface AtendimentoRepository extends Repository<Atendimento, Long> {
    Atendimento save(Atendimento atendimento);

    Optional<Atendimento> findById(Long id);

    /** ATD-04 · O atendimento a corrigir, travado: correções simultâneas do mesmo registro entram em fila. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Atendimento a where a.id = :id")
    Optional<Atendimento> buscarParaAlteracao(Long id);

    /** CAR-08 · Os atendimentos do associado, travados na ordem do id, para a anonimização. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Atendimento a where a.associadoId = :associadoId order by a.id")
    List<Atendimento> buscarDoAssociadoParaAlteracao(Long associadoId);

    List<Atendimento> findByAssociadoId(Long associadoId);

    /** ATD-03: já existe mensagem de aniversário da pessoa no ano (de qualquer autor). */
    boolean existsByAssociadoIdAndAnoAniversario(Long associadoId, Integer anoAniversario);

    /** ATD-04: um atendimento tem no máximo uma correção (também garantido por índice único). */
    boolean existsByCorrigeAtendimentoId(Long atendimentoId);

    @Query(
            """
             select count(a) from Atendimento a
             where a.realizadoEm >= :#{#periodo.inicio()} and a.realizadoEm < :#{#periodo.fim()}
               and (:#{#periodo.vendedorId()} is null or exists (
                 select c from ContratoNaFila c where c.vendedorId = :#{#periodo.vendedorId()}
                 and c.associadoId = a.associadoId and (a.contratoId is null or c.contratoId = a.contratoId)))
            """)
    long contarNoPeriodo(PeriodoDeAtendimentos periodo);

    @Query(
            """
             select distinct a.associadoId from Atendimento a
             where a.tipoContatoId = :tipo and a.resultado = :resultado
               and a.realizadoEm >= :#{#periodo.inicio()} and a.realizadoEm < :#{#periodo.fim()}
               and (:#{#periodo.vendedorId()} is null or exists (
                 select c from ContratoNaFila c where c.vendedorId = :#{#periodo.vendedorId()} and c.associadoId = a.associadoId))
            """)
    Set<Long> aniversariosRegistrados(
            Short tipo, ResultadoContato resultado, PeriodoDeAtendimentos periodo);

    default Set<Long> aniversariosRegistrados(Short tipo, PeriodoDeAtendimentos periodo) {
        return aniversariosRegistrados(tipo, ResultadoContato.CONCLUIDO, periodo);
    }
}
