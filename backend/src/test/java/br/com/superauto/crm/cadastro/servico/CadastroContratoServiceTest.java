package br.com.superauto.crm.cadastro.servico;

import static br.com.superauto.crm.Identificadores.comId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.Cadastrante;
import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.ContratoDeTeste;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.Mensalidade;
import br.com.superauto.crm.cadastro.entidade.NovoAssociado;
import br.com.superauto.crm.cadastro.entidade.Placa;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.entidade.Veiculo;
import br.com.superauto.crm.cadastro.repositorio.AssociadoRepository;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;
import br.com.superauto.crm.cadastro.repositorio.Localidade;
import br.com.superauto.crm.cadastro.repositorio.ReferenciasDoCadastro;
import br.com.superauto.crm.cadastro.repositorio.VeiculoRepository;
import br.com.superauto.crm.cadastro.repositorio.VendedoresDaEquipe;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CadastroContratoServiceTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-03-20T15:00:00Z"), SAO_PAULO);
    private static final Documento CPF = Documento.de("529.982.247-25");
    private static final Integer FORMIGA = 2535;
    private static final Short CARRO = 1;

    private final AssociadoRepository associados = mock(AssociadoRepository.class);
    private final VeiculoRepository veiculos = mock(VeiculoRepository.class);
    private final ContratoRepository contratos = mock(ContratoRepository.class);
    private final ReferenciasDoCadastro referencias = mock(ReferenciasDoCadastro.class);
    private final VendedoresDaEquipe vendedores = mock(VendedoresDaEquipe.class);
    private final CadastroContratoService servico = new CadastroContratoService(associados, veiculos, contratos,
            referencias, vendedores, RELOGIO);
    private final UUID vendedor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(referencias.tipoVeiculoAtivo(CARRO)).thenReturn(true);
        when(referencias.municipio(FORMIGA)).thenReturn(Optional.of(new Localidade(FORMIGA, "Formiga", "MG")));
        when(associados.findByDocumento(any())).thenReturn(Optional.empty());
        when(veiculos.findByPlaca(any())).thenReturn(Optional.empty());
        when(associados.save(any())).thenAnswer(i -> comId(i.getArgument(0), 5L));
        when(veiculos.save(any())).thenAnswer(i -> comId(i.getArgument(0), 6L));
        when(contratos.save(any())).thenAnswer(i -> comId(i.getArgument(0), 9L));
    }

    @Test
    @DisplayName("CAR-02: pessoa nova é cadastrada com veículo e contrato \"a conferir\" do vendedor que cadastra")
    void pessoaNova() {
        var registrado = servico.cadastrar(cadastro(Optional.of(pessoa())), new Cadastrante(vendedor, false));

        assertThat(registrado).isEqualTo(new ContratoRegistrado(9L, 5L, true));
        var contrato = contratoSalvo();
        assertThat(contrato.getSituacao()).isEqualTo(SituacaoContrato.A_CONFERIR);
        assertThat(contrato.getVendedorId()).isEqualTo(vendedor);
        assertThat(contrato.getAderidoEm().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 13));
    }

    @Test
    @DisplayName("CAR-02: dado um CPF já cadastrado, carrega a pessoa e não cria um segundo cadastro")
    void cpfJaCadastrado() {
        var existente = comId(Associado.cadastrar(new NovoAssociado(CPF,
                pessoa().dados(), pessoa().whatsapp(), Optional.empty()), vendedor), 3L);
        when(associados.findByDocumento(CPF)).thenReturn(Optional.of(existente));

        var registrado = servico.cadastrar(cadastro(Optional.empty()), new Cadastrante(vendedor, false));

        assertThat(registrado.associadoId()).isEqualTo(3L);
        assertThat(registrado.associadoNovo()).isFalse();
        verify(associados, never()).save(any());
    }

    @Test
    @DisplayName("CAR-02: dada uma placa com contrato ativo, recusa e devolve o caminho para a ficha existente")
    void placaComContratoVigente() {
        var veiculo = comId(Veiculo.cadastrar(Placa.de("HAA7216"), "Gol", CARRO), 6L);
        when(veiculos.findByPlaca(Placa.de("HAA7216"))).thenReturn(Optional.of(veiculo));
        var vigente = comId(ContratoDeTeste.vigente(77L), 40L);
        when(contratos.findFirstByVeiculoIdAndSituacaoNot(6L, SituacaoContrato.CANCELADO)).thenReturn(Optional.of(vigente));

        assertThatThrownBy(() -> servico.cadastrar(cadastro(Optional.of(pessoa())), new Cadastrante(vendedor, false)))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.CONFLITO);
                    assertThat(e.getCodigo()).isEqualTo("placa-com-contrato-vigente");
                    assertThat(e.getDados()).containsEntry("associadoId", 77L).containsEntry("contratoId", 40L);
                });
        verify(contratos, never()).save(any());
    }

    @Test
    @DisplayName("CAR-02: placa de contrato cancelado ganha contrato novo com o mesmo veículo")
    void placaComContratoCancelado() {
        var veiculo = comId(Veiculo.cadastrar(Placa.de("HAA7216"), "Gol", CARRO), 6L);
        when(veiculos.findByPlaca(Placa.de("HAA7216"))).thenReturn(Optional.of(veiculo));

        servico.cadastrar(cadastro(Optional.of(pessoa())), new Cadastrante(vendedor, false));

        assertThat(contratoSalvo().getVeiculoId()).isEqualTo(6L);
        assertThat(veiculo.getModelo()).isEqualTo("Onix 1.0");
        verify(veiculos, never()).save(any());
    }

    @Test
    @DisplayName("CAR-02: o gerencial escolhe o vendedor, que precisa ser um vendedor ativo")
    void gerencialEscolheVendedor() {
        UUID gerente = UUID.randomUUID();

        servico.cadastrar(cadastro(Optional.of(pessoa()), Optional.of(vendedor)), new Cadastrante(gerente, true));

        verify(vendedores).exigirVendedorAtivo(vendedor);
        assertThat(contratoSalvo().getVendedorId()).isEqualTo(vendedor);
    }

    @Test
    @DisplayName("Pessoa nova sem os dados pessoais é recusada")
    void pessoaNovaSemDados() {
        assertThatThrownBy(() -> servico.cadastrar(cadastro(Optional.empty()), new Cadastrante(vendedor, false)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("dados-pessoa-obrigatorios"));
    }

    @Test
    @DisplayName("Tipo de veículo inativo ou inexistente é recusado")
    void tipoVeiculoInvalido() {
        when(referencias.tipoVeiculoAtivo(CARRO)).thenReturn(false);

        assertThatThrownBy(() -> servico.cadastrar(cadastro(Optional.of(pessoa())), new Cadastrante(vendedor, false)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("tipo-veiculo-invalido"));
    }

    private Contrato contratoSalvo() {
        var captor = ArgumentCaptor.forClass(Contrato.class);
        verify(contratos).save(captor.capture());
        return captor.getValue();
    }

    private static NovaPessoa pessoa() {
        return new NovaPessoa(new DadosPessoais("José da Silva", null, FORMIGA), Telefone.de("37998692950"),
                Optional.empty());
    }

    private static NovoCadastro cadastro(Optional<NovaPessoa> pessoa) {
        return cadastro(pessoa, Optional.empty());
    }

    private static NovoCadastro cadastro(Optional<NovaPessoa> pessoa, Optional<UUID> vendedorEscolhido) {
        var condicoes = new CondicoesContrato(Mensalidade.de("124.82"), LocalDate.of(2026, 3, 13), Optional.empty(),
                Optional.empty(), vendedorEscolhido);
        return new NovoCadastro(CPF, pessoa, new NovoVeiculo(Placa.de("haa-7216"), "Onix 1.0", CARRO), condicoes);
    }
}
