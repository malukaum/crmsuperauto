package br.com.superauto.crm.referencia.servico;

import java.util.List;

import br.com.superauto.crm.referencia.repositorio.EtapaRelacionamentoRepository;
import br.com.superauto.crm.referencia.repositorio.MunicipioRepository;
import br.com.superauto.crm.referencia.repositorio.TipoVeiculoRepository;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listas de apoio das telas: cidades para escolher no cadastro, tipos de veículo e etapas da régua (filtros). */
@Service
@Transactional(readOnly = true)
public class ConsultaReferenciasService {

    /** Sugestões de cidade enquanto a pessoa digita: poucas bastam. */
    private static final Limit SUGESTOES_DE_CIDADE = Limit.of(20);
    private static final int MINIMO_PARA_BUSCAR = 2;

    private final MunicipioRepository municipios;
    private final TipoVeiculoRepository tiposVeiculo;
    private final EtapaRelacionamentoRepository etapas;

    public ConsultaReferenciasService(MunicipioRepository municipios, TipoVeiculoRepository tiposVeiculo,
                                      EtapaRelacionamentoRepository etapas) {
        this.municipios = municipios;
        this.tiposVeiculo = tiposVeiculo;
        this.etapas = etapas;
    }

    public record Cidade(Integer id, String nome, String uf) {
    }

    public record Opcao(Short id, String codigo, String nome) {
    }

    public List<Cidade> buscarCidades(String trecho) {
        if (trecho == null || trecho.isBlank() || trecho.strip().length() < MINIMO_PARA_BUSCAR) {
            return List.of();
        }
        return municipios.buscarPorNome(trecho.strip(), SUGESTOES_DE_CIDADE).stream()
                .map(m -> new Cidade(m.getId(), m.getNome(), m.getUfSigla()))
                .toList();
    }

    public List<Opcao> tiposVeiculo() {
        return tiposVeiculo.findByAtivoTrueOrderByDescricao().stream()
                .map(t -> new Opcao(t.getId(), t.getCodigo(), t.getDescricao()))
                .toList();
    }

    public List<Opcao> etapasDaRegua() {
        return etapas.findAllByOrderByOrdem().stream()
                .map(e -> new Opcao(e.getId(), e.getCodigo(), e.getNome()))
                .toList();
    }
}
