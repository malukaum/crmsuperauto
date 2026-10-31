package br.com.superauto.crm.cadastro.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import br.com.superauto.crm.cadastro.entidade.Adesao;
import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.Cadastrante;
import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.NovoAssociado;
import br.com.superauto.crm.cadastro.entidade.NovoContrato;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.entidade.Veiculo;
import br.com.superauto.crm.cadastro.repositorio.AssociadoRepository;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;
import br.com.superauto.crm.cadastro.repositorio.ReferenciasDoCadastro;
import br.com.superauto.crm.cadastro.repositorio.VeiculoRepository;
import br.com.superauto.crm.cadastro.repositorio.VendedoresDaEquipe;
import br.com.superauto.crm.erro.NegocioException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CAR-02 · Cadastrar cliente, veículo e contrato. Coordena três agregados numa só transação, como a história
 * pede: pessoa, veículo, contrato e régua são gravados juntos, e se algo falha nada é gravado. A régua é aberta
 * pelo Relacionamento ao ouvir {@code ContratoCadastrado}, antes do commit.
 */
@Service
public class CadastroContratoService {

    private final AssociadoRepository associados;
    private final VeiculoRepository veiculos;
    private final ContratoRepository contratos;
    private final ReferenciasDoCadastro referencias;
    private final VendedoresDaEquipe vendedores;
    private final Clock relogio;

    public CadastroContratoService(AssociadoRepository associados, VeiculoRepository veiculos,
                                   ContratoRepository contratos, ReferenciasDoCadastro referencias,
                                   VendedoresDaEquipe vendedores, Clock relogio) {
        this.associados = associados;
        this.veiculos = veiculos;
        this.contratos = contratos;
        this.referencias = referencias;
        this.vendedores = vendedores;
        this.relogio = relogio;
    }

    @Transactional
    public ContratoRegistrado cadastrar(NovoCadastro cadastro, Cadastrante cadastrante) {
        var existente = associados.findByDocumento(cadastro.documento());
        Associado associado = existente.orElseGet(() -> cadastrarPessoa(cadastro, cadastrante));
        Veiculo veiculo = veiculoSemContratoVigente(cadastro.veiculo());
        Contrato contrato = contratos.save(Contrato.cadastrar(
                novoContrato(associado, veiculo, cadastro.contrato(), cadastrante), hoje(), cadastrante.id()));
        return new ContratoRegistrado(contrato.getId(), associado.getId(), existente.isEmpty());
    }

    private Associado cadastrarPessoa(NovoCadastro cadastro, Cadastrante cadastrante) {
        NovaPessoa pessoa = cadastro.pessoa().orElseThrow(() -> NegocioException.invalido("dados-pessoa-obrigatorios",
                "Informe nome, WhatsApp e cidade para cadastrar a pessoa."));
        exigirMunicipio(pessoa.dados().municipioId());
        var novo = new NovoAssociado(cadastro.documento(), pessoa.dados(), pessoa.whatsapp(), pessoa.email());
        return associados.save(Associado.cadastrar(novo, cadastrante.id()));
    }

    /** A placa com contrato não cancelado é recusada, com o caminho para a ficha existente. */
    private Veiculo veiculoSemContratoVigente(NovoVeiculo dados) {
        if (!referencias.tipoVeiculoAtivo(dados.tipoVeiculoId())) {
            throw NegocioException.invalido("tipo-veiculo-invalido", "Escolha um tipo de veículo válido.");
        }
        return veiculos.findByPlaca(dados.placa())
                .map(veiculo -> {
                    exigirSemContratoVigente(veiculo);
                    veiculo.descrever(dados.modelo(), dados.tipoVeiculoId());
                    return veiculo;
                })
                .orElseGet(() -> veiculos.save(Veiculo.cadastrar(dados.placa(), dados.modelo(), dados.tipoVeiculoId())));
    }

    private void exigirSemContratoVigente(Veiculo veiculo) {
        contratos.findFirstByVeiculoIdAndSituacaoNot(veiculo.getId(), SituacaoContrato.CANCELADO).ifPresent(vigente -> {
            throw NegocioException.conflito("placa-com-contrato-vigente",
                    "A placa " + veiculo.getPlaca() + " já tem um contrato que não está cancelado.",
                    Map.of("associadoId", vigente.getAssociadoId(), "contratoId", vigente.getId()));
        });
    }

    private NovoContrato novoContrato(Associado associado, Veiculo veiculo, CondicoesContrato condicoes,
                                      Cadastrante cadastrante) {
        var vendedor = cadastrante.vendedorDoContrato(condicoes.vendedorId());
        if (cadastrante.gerencial()) {
            vendedores.exigirVendedorAtivo(vendedor);
        }
        condicoes.codigoSga().filter(contratos::existsByCodigoSga).ifPresent(codigo -> {
            throw NegocioException.conflito("codigo-sga-repetido", "O código do SGA " + codigo + " já está em outro contrato.");
        });
        var adesao = new Adesao(associado.getId(), veiculo.getId(), condicoes.mensalidade(), inicioDoDia(condicoes.dataAdesao()));
        return new NovoContrato(adesao, vendedor, condicoes.dataVenda().map(this::inicioDoDia), condicoes.codigoSga());
    }

    private void exigirMunicipio(Integer municipioId) {
        if (referencias.municipio(municipioId).isEmpty()) {
            throw NegocioException.invalido("cidade-invalida", "Escolha uma cidade da lista.");
        }
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    /** Datas de calendário viram o início do dia em São Paulo (RG-01). */
    private OffsetDateTime inicioDoDia(LocalDate data) {
        return data.atStartOfDay(relogio.getZone()).toOffsetDateTime();
    }
}
