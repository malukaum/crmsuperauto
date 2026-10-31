package br.com.superauto.crm.cadastro.repositorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositório do agregado {@link Contrato}. */
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    @Query("""
            select new br.com.superauto.crm.cadastro.repositorio.TotalPorVendedor(c.vendedorId, count(c))
              from Contrato c
             where c.vendedorId is not null and c.situacao <> :excluida
             group by c.vendedorId
            """)
    List<TotalPorVendedor> contarPorVendedorExceto(SituacaoContrato excluida);

    long countByVendedorIdAndSituacaoNot(UUID vendedorId, SituacaoContrato situacao);

    List<Contrato> findByVendedorIdAndSituacaoNot(UUID vendedorId, SituacaoContrato situacao);

    /** O contrato não cancelado do veículo, se houver: um veículo só tem um vigente por vez. */
    Optional<Contrato> findFirstByVeiculoIdAndSituacaoNot(Long veiculoId, SituacaoContrato situacao);

    boolean existsByCodigoSga(String codigoSga);

    boolean existsByCodigoSgaAndIdNot(String codigoSga, Long id);

    List<Contrato> findByAssociadoId(Long associadoId);

    /** RG-03: o vendedor só enxerga a pessoa se tiver algum contrato dela. */
    boolean existsByAssociadoIdAndVendedorId(Long associadoId, UUID vendedorId);
}
