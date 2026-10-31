package br.com.superauto.crm.referencia.infraestrutura.persistencia;

import java.util.Optional;

import br.com.superauto.crm.cadastro.repositorio.Localidade;
import br.com.superauto.crm.cadastro.repositorio.ReferenciasDoCadastro;
import br.com.superauto.crm.referencia.repositorio.MunicipioRepository;
import br.com.superauto.crm.referencia.repositorio.TipoVeiculoRepository;

import org.springframework.stereotype.Component;

/** A Referência, dona dos municípios e dos tipos de veículo, atende a porta {@link ReferenciasDoCadastro}. */
@Component
public class ReferenciasDoCadastroJpa implements ReferenciasDoCadastro {

    private final MunicipioRepository municipios;
    private final TipoVeiculoRepository tiposVeiculo;

    public ReferenciasDoCadastroJpa(MunicipioRepository municipios, TipoVeiculoRepository tiposVeiculo) {
        this.municipios = municipios;
        this.tiposVeiculo = tiposVeiculo;
    }

    @Override
    public Optional<Localidade> municipio(Integer municipioId) {
        return municipios.findById(municipioId).map(m -> new Localidade(m.getId(), m.getNome(), m.getUfSigla()));
    }

    @Override
    public boolean tipoVeiculoAtivo(Short tipoVeiculoId) {
        return tiposVeiculo.existsByIdAndAtivoTrue(tipoVeiculoId);
    }
}
