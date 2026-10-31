package br.com.superauto.crm.relacionamento.infraestrutura.persistencia;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.superauto.crm.cadastro.repositorio.EtapaPrevista;
import br.com.superauto.crm.cadastro.repositorio.EtapaPrevista.SituacaoEtapa;
import br.com.superauto.crm.cadastro.repositorio.ReguaDosContratos;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.EtapaNaRegua;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.servico.PrevisaoRegua;

import org.springframework.stereotype.Component;

/** O Relacionamento, dono do acompanhamento dos contratos, atende a porta {@link ReguaDosContratos} do Cadastro. */
@Component
public class ReguaDosContratosJpa implements ReguaDosContratos {

    private final AcompanhamentoContratoRepository acompanhamentos;
    private final ReguaDePosVenda regua;

    public ReguaDosContratosJpa(AcompanhamentoContratoRepository acompanhamentos, ReguaDePosVenda regua) {
        this.acompanhamentos = acompanhamentos;
        this.regua = regua;
    }

    @Override
    public Map<Long, List<EtapaPrevista>> previsoes(Collection<Long> contratoIds) {
        if (contratoIds.isEmpty()) {
            return Map.of();
        }
        List<EtapaRegua> etapas = regua.etapas();
        return acompanhamentos.findAllById(contratoIds).stream()
                .collect(Collectors.toUnmodifiableMap(AcompanhamentoContrato::getId, a -> prever(etapas, a)));
    }

    private static List<EtapaPrevista> prever(List<EtapaRegua> etapas, AcompanhamentoContrato acompanhamento) {
        return PrevisaoRegua.prever(etapas, acompanhamento.getEtapaPendenteId(), acompanhamento.getProximoContatoEm())
                .stream().map(ReguaDosContratosJpa::traduzir).toList();
    }

    private static EtapaPrevista traduzir(EtapaNaRegua etapa) {
        return new EtapaPrevista(etapa.etapa().codigo(), etapa.etapa().nome(),
                SituacaoEtapa.valueOf(etapa.andamento().name()), etapa.prevista());
    }
}
