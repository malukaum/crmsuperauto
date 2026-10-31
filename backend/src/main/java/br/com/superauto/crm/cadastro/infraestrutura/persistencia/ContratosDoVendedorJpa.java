package br.com.superauto.crm.cadastro.infraestrutura.persistencia;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import br.com.superauto.crm.acesso.repositorio.ContratosDoVendedor;
import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;
import br.com.superauto.crm.cadastro.repositorio.TotalPorVendedor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * O contexto Cadastro, dono dos contratos, implementa a porta {@link ContratosDoVendedor} que o contexto
 * Acesso declarou para a gestão de usuários. Acesso não conhece {@link Contrato} nem o repositório dele.
 */
@Component
public class ContratosDoVendedorJpa implements ContratosDoVendedor {

    private final ContratoRepository contratos;

    public ContratosDoVendedorJpa(ContratoRepository contratos) {
        this.contratos = contratos;
    }

    @Override
    public Map<UUID, Long> contarPorVendedor() {
        return contratos.contarPorVendedorExceto(SituacaoContrato.CANCELADO).stream()
                .collect(Collectors.toMap(TotalPorVendedor::vendedorId, TotalPorVendedor::total));
    }

    @Override
    public long contar(UUID vendedor) {
        return contratos.countByVendedorIdAndSituacaoNot(vendedor, SituacaoContrato.CANCELADO);
    }

    /**
     * Cada contrato muda de vendedor pela raiz do agregado; o {@code saveAll} publica os eventos
     * {@code ContratoTransferido}, que a auditoria registra na mesma transação.
     */
    @Override
    @Transactional
    public int transferir(UUID de, UUID para, UUID autor) {
        var vigentes = contratos.findByVendedorIdAndSituacaoNot(de, SituacaoContrato.CANCELADO);
        vigentes.forEach(contrato -> contrato.transferirPara(para, autor));
        contratos.saveAll(vigentes);
        return vigentes.size();
    }
}
