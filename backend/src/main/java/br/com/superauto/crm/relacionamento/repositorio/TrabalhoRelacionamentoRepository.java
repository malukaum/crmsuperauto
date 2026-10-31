package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Projeção de leitura compartilhada pelo painel e pelas filas. */
public interface TrabalhoRelacionamentoRepository extends Repository<ContratoNaFila, Long> {
    Optional<ContratoNaFila> findById(Long id);

    @Query(
            "select new"
                + " br.com.superauto.crm.relacionamento.repositorio.ContagemDaCarteira(c.tipoVeiculo,"
                + " c.situacao, count(c), sum(cast(c.mensalidade as BigDecimal))) from"
                + " ContratoNaFila c where (:vendedor is null or c.vendedorId = :vendedor) group by"
                + " c.tipoVeiculo, c.situacao")
    List<ContagemDaCarteira> contagens(UUID vendedor);

    @Query(
            "select c from ContratoNaFila c where c.associadoId in :associados and (:vendedor is"
                    + " null or c.vendedorId = :vendedor)")
    List<ContratoNaFila> dosAssociados(Set<Long> associados, UUID vendedor);

    @Query("select c from ContratoNaFila c where c.etapaId is not null order by c.contratoId")
    List<ContratoNaFila> comReguaAtiva();
}
