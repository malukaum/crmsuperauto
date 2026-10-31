package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Só leitura: o histórico vem de uma visão. */
public interface HistoricoAtendimentoRepository
        extends Repository<LinhaHistoricoAtendimento, Long> {

    List<LinhaHistoricoAtendimento> findByAssociadoIdOrderByRealizadoEmDescIdDesc(Long associadoId);

    @Query(
            "select h from LinhaHistoricoAtendimento h where h.realizadoEm >= :inicio and"
                + " h.realizadoEm < :fim and (:vendedor is null or exists (select c from"
                + " ContratoNaFila c where c.vendedorId = :vendedor and c.associadoId ="
                + " h.associadoId and (h.contratoId is null or c.contratoId = h.contratoId))) order"
                + " by h.realizadoEm desc, h.id desc")
    List<LinhaHistoricoAtendimento> noPeriodo(
            OffsetDateTime inicio, OffsetDateTime fim, UUID vendedor);
}
