package br.com.superauto.crm.referencia.infraestrutura.persistencia;

import java.util.List;

import br.com.superauto.crm.referencia.entidade.EtapaRelacionamento;
import br.com.superauto.crm.referencia.repositorio.EtapaRelacionamentoRepository;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;

import org.springframework.stereotype.Component;

/** A Referência, onde a régua é configurada, atende a porta {@link ReguaDePosVenda} do Relacionamento. */
@Component
public class ReguaDePosVendaJpa implements ReguaDePosVenda {

    private final EtapaRelacionamentoRepository etapas;

    public ReguaDePosVendaJpa(EtapaRelacionamentoRepository etapas) {
        this.etapas = etapas;
    }

    @Override
    public List<EtapaRegua> etapas() {
        return etapas.findAllByOrderByOrdem().stream().map(ReguaDePosVendaJpa::traduzir).toList();
    }

    private static EtapaRegua traduzir(EtapaRelacionamento etapa) {
        return new EtapaRegua(etapa.getId(), etapa.getCodigo(), etapa.getNome(), etapa.getDiasAposAnterior(),
                etapa.getProximaEtapaId(), etapa.isInicial());
    }
}
