package br.com.superauto.crm.cadastro.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.ContatoAssociado;
import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;
import br.com.superauto.crm.cadastro.repositorio.AssociadoRepository;
import br.com.superauto.crm.cadastro.repositorio.AtendimentoDoHistorico;
import br.com.superauto.crm.cadastro.repositorio.ConsultaCarteira;
import br.com.superauto.crm.cadastro.repositorio.EtapaPrevista;
import br.com.superauto.crm.cadastro.repositorio.HistoricoDeAtendimentos;
import br.com.superauto.crm.cadastro.repositorio.IndicacoesDoAssociado;
import br.com.superauto.crm.cadastro.repositorio.Localidade;
import br.com.superauto.crm.cadastro.repositorio.ReferenciasDoCadastro;
import br.com.superauto.crm.cadastro.repositorio.ReguaDosContratos;
import br.com.superauto.crm.cadastro.servico.FichaAssociado.ContatoDaFicha;
import br.com.superauto.crm.cadastro.servico.FichaAssociado.ContratoDaFicha;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.erro.NegocioException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lado de leitura do associado: a busca pelo documento no início do cadastro (CAR-02) e a ficha (CAR-03).
 * O vendedor só abre a ficha de quem tem contrato dele e só vê esses contratos e os atendimentos deles (RG-03).
 */
@Service
@Transactional(readOnly = true)
public class ConsultaAssociadoService {

    private final AssociadoRepository associados;
    private final ConsultaCarteira carteira;
    private final ReferenciasDoCadastro referencias;
    private final ReguaDosContratos reguas;
    private final HistoricoDeAtendimentos historico;
    private final IndicacoesDoAssociado indicacoes;
    private final Clock relogio;

    public ConsultaAssociadoService(AssociadoRepository associados, ConsultaCarteira carteira,
                                    ReferenciasDoCadastro referencias, ReguaDosContratos reguas,
                                    HistoricoDeAtendimentos historico, IndicacoesDoAssociado indicacoes,
                                    Clock relogio) {
        this.associados = associados;
        this.carteira = carteira;
        this.referencias = referencias;
        this.reguas = reguas;
        this.historico = historico;
        this.indicacoes = indicacoes;
        this.relogio = relogio;
    }

    public Optional<PessoaCadastrada> porDocumento(Documento documento) {
        return associados.findByDocumento(documento).map(associado -> {
            Optional<Localidade> local = localidade(associado);
            return new PessoaCadastrada(associado.getId(), associado.getNome(),
                    local.map(Localidade::cidade).orElse(null), local.map(Localidade::uf).orElse(null));
        });
    }

    public FichaAssociado ficha(Long associadoId, EscopoCarteira escopo) {
        Associado associado = associados.findById(associadoId).orElseThrow(ConsultaAssociadoService::naoEncontrado);
        List<LinhaCarteira> contratos = carteira.doAssociado(associadoId).stream()
                .filter(linha -> escopo.enxergaContratoDe(linha.getVendedorId()))
                .toList();
        if (escopo.vendedor().isPresent() && contratos.isEmpty()) {
            throw naoEncontrado();
        }
        Set<Long> visiveis = contratos.stream().map(LinhaCarteira::getContratoId).collect(Collectors.toSet());
        return montar(associado, contratos, reguas.previsoes(visiveis), atendimentos(associadoId, escopo, visiveis));
    }

    /** O vendedor vê os atendimentos dos seus contratos e os contatos sobre a pessoa, como a mensagem de aniversário. */
    private List<AtendimentoDoHistorico> atendimentos(Long associadoId, EscopoCarteira escopo, Set<Long> visiveis) {
        return historico.doAssociado(associadoId).stream()
                .filter(a -> escopo.vendedor().isEmpty() || a.contratoId().map(visiveis::contains).orElse(true))
                .toList();
    }

    private FichaAssociado montar(Associado associado, List<LinhaCarteira> contratos,
                                  Map<Long, List<EtapaPrevista>> previsoes, List<AtendimentoDoHistorico> atendimentos) {
        Optional<Localidade> local = localidade(associado);
        var classificacao = associado.classificacao();
        return new FichaAssociado(associado.getId(), associado.getNome(),
                associado.getDocumento().map(Documento::mascarado).orElse(null),
                associado.getDocumento().map(Documento::pessoaFisica).orElse(false),
                associado.getDataNascimento().orElse(null), associado.getMunicipioId(),
                local.map(Localidade::cidade).orElse(null), local.map(Localidade::uf).orElse(null),
                classificacao.perfil(), classificacao.potencial(), associado.anonimizado(), associado.getVersao(),
                associado.getContatos().stream().map(ConsultaAssociadoService::contato).toList(),
                contratos.stream().map(linha -> contrato(linha, previsoes.getOrDefault(linha.getContratoId(), List.of()))).toList(),
                atendimentos, indicacoes.feitasPor(associado.getId()));
    }

    private static ContatoDaFicha contato(ContatoAssociado contato) {
        var meio = contato.getMeio();
        return new ContatoDaFicha(contato.getId(), meio.tipo(), meio.valor(), meio.exibicao(), contato.isPrincipal(),
                contato.isAceitaContato());
    }

    private ContratoDaFicha contrato(LinhaCarteira linha, List<EtapaPrevista> regua) {
        return new ContratoDaFicha(linha.getContratoId(), linha.getContratoVersao(), linha.getPlaca().valor(),
                linha.getModelo(), linha.getTipoVeiculoId(), linha.getTipoVeiculo(), linha.getMensalidade().valor(),
                linha.getSituacao(), linha.getCodigoSga().orElse(null), dia(linha.getAderidoEm()),
                linha.getVendidoEm().map(this::dia).orElse(null), linha.getEncerradoEm().map(this::dia).orElse(null),
                linha.getVendedorId().orElse(null), linha.getVendedorNome().orElse(null), linha.getFila(),
                linha.getDiasAtraso(), linha.getEtapaPendente().orElse(null),
                linha.getProximoContatoEm().orElse(null), regua);
    }

    private Optional<Localidade> localidade(Associado associado) {
        return Optional.ofNullable(associado.getMunicipioId()).flatMap(referencias::municipio);
    }

    /** O dia em São Paulo (RG-01). */
    private LocalDate dia(OffsetDateTime instante) {
        return instante.atZoneSameInstant(relogio.getZone()).toLocalDate();
    }

    private static NegocioException naoEncontrado() {
        return NegocioException.naoEncontrado("Cliente não encontrado na carteira.");
    }
}
