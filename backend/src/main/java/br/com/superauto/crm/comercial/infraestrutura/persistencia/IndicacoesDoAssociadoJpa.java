package br.com.superauto.crm.comercial.infraestrutura.persistencia;

import java.util.List;

import br.com.superauto.crm.cadastro.repositorio.IndicacaoFeita;
import br.com.superauto.crm.cadastro.repositorio.IndicacoesDoAssociado;
import br.com.superauto.crm.comercial.repositorio.IndicacaoResumidaRepository;

import org.springframework.stereotype.Component;

/** O Comercial, dono das indicações, atende a porta {@link IndicacoesDoAssociado} do Cadastro. */
@Component
public class IndicacoesDoAssociadoJpa implements IndicacoesDoAssociado {

    private final IndicacaoResumidaRepository indicacoes;

    public IndicacoesDoAssociadoJpa(IndicacaoResumidaRepository indicacoes) {
        this.indicacoes = indicacoes;
    }

    @Override
    public List<IndicacaoFeita> feitasPor(Long associadoId) {
        return indicacoes.findByIndicadorIdOrderByRegistradaEmDesc(associadoId).stream()
                .map(i -> new IndicacaoFeita(i.getId(), i.getIndicadoNome(), i.getStatus(), i.getRegistradaEm()))
                .toList();
    }
}
