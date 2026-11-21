package br.com.superauto.crm.assistente.repositorio;

import br.com.superauto.crm.assistente.entidade.AnotacaoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.SinalDoAtendimento;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repositório do agregado {@link AnotacaoDoAtendimento}. */
public interface AnotacaoDoAtendimentoRepository extends JpaRepository<AnotacaoDoAtendimento, Long> {

    /** RG-06 · Carrega travando a linha até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AnotacaoDoAtendimento a where a.atendimentoId = :atendimentoId")
    Optional<AnotacaoDoAtendimento> buscarParaAlteracao(Long atendimentoId);

    /** CAR-08 · As anotações do associado, travadas na ordem do id, para a anonimização. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AnotacaoDoAtendimento a where a.associadoId = :associadoId order by a.atendimentoId")
    List<AnotacaoDoAtendimento> buscarDoAssociadoParaAlteracao(Long associadoId);

    /** IA-01 · Anotações aguardando resumo cuja vez já chegou, das mais antigas para as mais novas. */
    @Query("""
            select a.atendimentoId from AnotacaoDoAtendimento a
             where a.situacao = br.com.superauto.crm.assistente.entidade.SituacaoDoResumo.AGUARDANDO
               and (a.proximaTentativaEm is null or a.proximaTentativaEm <= :agora)
             order by a.registradoEm, a.atendimentoId
            """)
    List<Long> pendentes(OffsetDateTime agora, Limit limite);

    List<AnotacaoDoAtendimento> findByAssociadoIdOrderByRegistradoEmDesc(Long associadoId);

    /** IA-03 · Os sinais dos resumos do período (toda a carteira). */
    @Query("""
            select new br.com.superauto.crm.assistente.entidade.SinalDoAtendimento(
                   a.assuntos, a.satisfacao, a.riscoCancelamento, a.chanceIndicar)
              from AnotacaoDoAtendimento a
             where a.situacao = br.com.superauto.crm.assistente.entidade.SituacaoDoResumo.RESUMIDO
               and a.resumidoEm >= :inicio and a.resumidoEm < :fim
            """)
    List<SinalDoAtendimento> sinais(OffsetDateTime inicio, OffsetDateTime fim);

    /** IA-03 / RG-03 · Os sinais do período só dos associados informados (a carteira do vendedor). */
    @Query("""
            select new br.com.superauto.crm.assistente.entidade.SinalDoAtendimento(
                   a.assuntos, a.satisfacao, a.riscoCancelamento, a.chanceIndicar)
              from AnotacaoDoAtendimento a
             where a.situacao = br.com.superauto.crm.assistente.entidade.SituacaoDoResumo.RESUMIDO
               and a.resumidoEm >= :inicio and a.resumidoEm < :fim
               and a.associadoId in :associados
            """)
    List<SinalDoAtendimento> sinaisDosAssociados(OffsetDateTime inicio, OffsetDateTime fim, Collection<Long> associados);
}
