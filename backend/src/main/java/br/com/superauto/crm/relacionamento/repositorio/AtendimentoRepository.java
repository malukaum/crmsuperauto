package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;

/** Histórico preservado: o repositório do agregado não expõe exclusão (RG-07). */
public interface AtendimentoRepository extends Repository<Atendimento, Long> {
    Atendimento save(Atendimento atendimento);

    List<Atendimento> findByAssociadoId(Long associadoId);

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
