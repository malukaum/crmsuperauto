package br.com.superauto.crm.relacionamento;

import static org.assertj.core.api.Assertions.*;

import br.com.superauto.crm.acesso.entidade.Usuario;
import br.com.superauto.crm.cadastro.entidade.Adesao;
import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.NovoAssociado;
import br.com.superauto.crm.cadastro.entidade.NovoContrato;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.entidade.Veiculo;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.referencia.entidade.PrazoDaEtapaAlterado;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoAtualizado;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.AtendimentoRegistrado;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDeAniversarios;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ConsultaFilas;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.servico.AutorDoContato;
import br.com.superauto.crm.relacionamento.servico.ConfiguracaoReguaService;
import br.com.superauto.crm.relacionamento.servico.ConsultaTrabalhoService;
import br.com.superauto.crm.relacionamento.servico.ContadoresFilasService;
import br.com.superauto.crm.relacionamento.servico.LinhaDeTrabalho;
import br.com.superauto.crm.relacionamento.servico.RegistroDeContato;
import br.com.superauto.crm.relacionamento.servico.RegistroNaFilaService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/** Integração opt-in com Postgres local; cada cenário desfaz seus dados no fim. */
@SpringBootTest
@RecordApplicationEvents
@Transactional
@EnabledIfEnvironmentVariable(named = "FIL_TESTAR_BANCO", matches = "true")
class FilasPostgresTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 31);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @TestConfiguration
    static class Datas {
        @Bean
        @Primary
        Clock relogioDeTeste() {
            return Clock.fixed(HOJE.atTime(12, 0).atZone(FUSO).toInstant(), FUSO);
        }
    }

    @Autowired EntityManager em;
    @Autowired ReguaDePosVenda regua;
    @Autowired ConsultaTrabalhoService consulta;
    @Autowired RegistroNaFilaService registros;
    @Autowired ConfiguracaoReguaService configuracao;
    @Autowired AcompanhamentoContratoRepository acompanhamentos;
    @Autowired ContadoresFilasService contadores;
    @Autowired ApplicationEvents eventos;
    @Autowired ConsultaFilas consultaFilas;
    private UUID vendedor;
    private AutorDoContato autor;
    private Associado associado;
    private int numeroPlaca;

    @BeforeEach
    void preparar() {
        vendedor = UUID.randomUUID();
        autor = new AutorDoContato(vendedor, Optional.of(vendedor));
        em.persist(new Usuario(vendedor, "teste-fil-" + vendedor, "Teste FIL", null));
        associado =
                Associado.cadastrar(
                        new NovoAssociado(
                                Documento.de(cpf()),
                                new DadosPessoais(
                                        "José FIL",
                                        LocalDate.of(1992, 10, 31),
                                        em.createQuery(
                                                        "select m.id from Municipio m where m.nome"
                                                                + " = :nome",
                                                        Integer.class)
                                                .setParameter("nome", "Formiga")
                                                .getSingleResult()),
                                Telefone.de("37998692950"),
                                Optional.empty()),
                        vendedor);
        em.persist(associado);
        numeroPlaca = new Random().nextInt(9000);
    }

    private Contrato contrato(LocalDate adesao, SituacaoContrato situacao) {
        var veiculo =
                Veiculo.cadastrar(
                        Placa.de("ZZZ" + String.format("%04d", numeroPlaca++)),
                        "Veículo FIL",
                        (short) 1);
        em.persist(veiculo);
        var contrato =
                Contrato.cadastrar(
                        new NovoContrato(
                                new Adesao(
                                        associado.getId(),
                                        veiculo.getId(),
                                        Mensalidade.de("100.00"),
                                        adesao.atStartOfDay(FUSO).toOffsetDateTime()),
                                vendedor,
                                Optional.empty(),
                                Optional.empty()),
                        HOJE,
                        vendedor);
        em.persist(contrato);
        contrato.alterarSituacao(situacao, HOJE.atStartOfDay(FUSO).toOffsetDateTime(), vendedor);
        em.persist(AcompanhamentoContrato.iniciar(contrato.getId(), regua.etapas(), adesao));
        em.flush();
        return contrato;
    }

    private List<LinhaDeTrabalho> fila(boolean atrasados) {
        return consulta.contatos(
                atrasados, new FiltroDaFila(null, null, null, null), Optional.of(vendedor));
    }

    private void registrar(Contrato c, ResultadoContato resultado, LocalDate retorno) {
        registros.registrar(
                c.getId(), new RegistroDeContato(0, resultado, retorno, "Teste de contato"), autor);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName(
            "FIL-02: conclusão grava histórico e remove da fila e do contador; suspenso fica fora")
    void concluirNaFila() {
        var contrato = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        contrato(HOJE.minusDays(7), SituacaoContrato.SUSPENSO);
        assertThat(fila(false)).hasSize(1);
        assertThat(contadores.contar(Optional.of(vendedor)).contatosHoje()).isEqualTo(1);
        registrar(contrato, ResultadoContato.CONCLUIDO, null);
        assertThat(fila(false)).isEmpty();
        assertThat(contadores.contar(Optional.of(vendedor)).contatosHoje()).isZero();
        assertThat(consulta.atendimentosMes(Optional.of(vendedor)))
                .singleElement()
                .satisfies(a -> assertThat(a.resultado()).isEqualTo("CONCLUIDO"));
        assertThat(acompanhamentos.findById(contrato.getId()).orElseThrow().getProximoContatoEm())
                .contains(HOJE.plusDays(23));
    }

    @Test
    @DisplayName("FIL-03: ordena pelo maior atraso e Sem resposta agenda daqui a dois dias")
    void recuperarAtrasados() {
        var antigo = contrato(HOJE.minusDays(100), SituacaoContrato.ATIVO);
        contrato(HOJE.minusDays(10), SituacaoContrato.A_CONFERIR);
        assertThat(fila(true)).extracting(LinhaDeTrabalho::diasAtraso).containsExactly(93L, 3L);
        registrar(antigo, ResultadoContato.SEM_RESPOSTA, null);
        assertThat(fila(true)).hasSize(1);
        assertThat(acompanhamentos.findById(antigo.getId()).orElseThrow().getProximoContatoEm())
                .contains(LocalDate.of(2026, 11, 2));
    }

    @Test
    @DisplayName(
            "FIL-04: duas placas geram uma pessoa; mensagem não altera as réguas e avisa repetição")
    void aniversarioDaPessoa() {
        var primeiro = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        contrato(HOJE.minusDays(10), SituacaoContrato.A_CONFERIR);
        assertThat(consulta.aniversarios(0, Optional.of(vendedor)))
                .singleElement()
                .satisfies(
                        l -> {
                            assertThat(l.placa()).contains(" / ");
                            assertThat(l.mensalidade()).isEqualByComparingTo("200.00");
                        });
        registros.aniversario(primeiro.getId(), false, autor);
        em.flush();
        em.clear();
        assertThat(consulta.aniversarios(0, Optional.of(vendedor)))
                .singleElement()
                .satisfies(l -> assertThat(l.mensagemEnviada()).isTrue());
        assertThat(fila(false)).hasSize(1);
        assertThat(fila(true)).hasSize(1);
        assertThatThrownBy(() -> registros.aniversario(primeiro.getId(), false, autor))
                .isInstanceOfSatisfying(
                        NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("aniversario-ja-registrado"));
    }

    @Test
    @DisplayName(
            "FIL-06: prévia e recálculo usam o último contato concluído e preservam o retorno"
                    + " manual")
    void recalcularComPrevia() {
        var automatico = contrato(HOJE.minusDays(10), SituacaoContrato.ATIVO);
        var manual = contrato(HOJE.minusDays(10), SituacaoContrato.ATIVO);
        registrar(automatico, ResultadoContato.CONCLUIDO, null);
        registrar(manual, ResultadoContato.REAGENDADO, LocalDate.of(2026, 11, 5));
        Map<Short, Integer> prazos = new LinkedHashMap<>();
        regua.etapas()
                .forEach(
                        e ->
                                prazos.put(
                                        e.id(),
                                        e.codigo().equals("D30") ? 25 : e.diasAposAnterior()));
        var previa = configuracao.prever(prazos);
        assertThat(previa.contratosAfetados()).isGreaterThanOrEqualTo(1);
        configuracao.salvar(prazos, previa.confirmacao(), vendedor);
        em.flush();
        em.clear();
        assertThat(acompanhamentos.findById(automatico.getId()).orElseThrow().getProximoContatoEm())
                .contains(HOJE.plusDays(25));
        assertThat(acompanhamentos.findById(manual.getId()).orElseThrow().getProximoContatoEm())
                .contains(LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("RG-03 e RG-06: outro vendedor e versão desatualizada não gravam")
    void escopoEVersao() {
        var contrato = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        var registro = new RegistroDeContato(0, ResultadoContato.PENDENTE, null, null);
        var outro = new AutorDoContato(UUID.randomUUID(), Optional.of(UUID.randomUUID()));
        assertThatThrownBy(() -> registros.registrar(contrato.getId(), registro, outro))
                .isInstanceOf(NegocioException.class);
        registrar(contrato, ResultadoContato.PENDENTE, null);
        assertThatThrownBy(() -> registros.registrar(contrato.getId(), registro, autor))
                .isInstanceOfSatisfying(
                        NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("edicao-simultanea"));
    }

    @Test
    @DisplayName("RG-05: nome sem acento e telefone com pontuação encontram o associado")
    void busca() {
        var contrato = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        for (String busca :
                List.of(
                        "jose",
                        em.find(Veiculo.class, contrato.getVeiculoId()).getPlaca().valor(),
                        "(37) 99869-2950"))
            assertThat(
                            consulta.contatos(
                                    false,
                                    new FiltroDaFila(busca, null, null, null),
                                    Optional.of(vendedor)))
                    .hasSize(1);
    }

    @Test
    @DisplayName("FIL-06: uma gravação após a prévia exige recalcular o impacto antes de salvar")
    void previaDesatualizada() {
        var contrato = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        Map<Short, Integer> prazos = new LinkedHashMap<>();
        regua.etapas().forEach(e -> prazos.put(e.id(), e.diasAposAnterior()));
        var previa = configuracao.prever(prazos);
        registrar(contrato, ResultadoContato.SEM_RESPOSTA, null);
        assertThatThrownBy(() -> configuracao.salvar(prazos, previa.confirmacao(), vendedor))
                .isInstanceOfSatisfying(
                        NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("previa-desatualizada"));
    }

    @Test
    @DisplayName("FIL-01: painel agrega só a carteira e os atendimentos do vendedor")
    void painelNoEscopo() {
        var contrato = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        contrato(HOJE.minusDays(10), SituacaoContrato.A_CONFERIR);
        contrato(HOJE.minusDays(7), SituacaoContrato.CANCELADO);
        var painel = consulta.painel(Optional.of(vendedor));
        assertThat(painel.carteira()).isEqualTo(2);
        assertThat(painel.contatosHoje()).isEqualTo(1);
        assertThat(painel.atrasados()).isEqualTo(1);
        assertThat(painel.aniversariantesHoje()).isEqualTo(1);
        assertThat(painel.mensalidadesAtivas()).isEqualByComparingTo("100.00");
        assertThat(painel.aConferir()).isEqualTo(1);
        registros.aniversario(contrato.getId(), false, autor);
        em.flush();
        em.clear();
        assertThat(consulta.painel(Optional.of(vendedor)).atendimentosMes()).isEqualTo(1);
        assertThat(consulta.painel(Optional.of(UUID.randomUUID())).carteira()).isZero();
    }

    @Test
    @DisplayName("FIL-01 / RG-07: anonimização preserva a contagem e o histórico de atendimentos")
    void historicoPreservadoAposAnonimizacao() {
        var contrato = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        registros.aniversario(contrato.getId(), false, autor);
        var agora = HOJE.atTime(12, 0).atZone(FUSO).toOffsetDateTime();
        contrato.alterarSituacao(SituacaoContrato.CANCELADO, agora, vendedor);
        associado.anonimizar(List.of(contrato), agora, vendedor);
        em.flush();
        em.clear();

        var painel = consulta.painel(Optional.of(vendedor));
        assertThat(painel.carteira()).isZero();
        assertThat(painel.aniversariantesHoje()).isZero();
        assertThat(painel.atendimentosMes()).isEqualTo(1);
        assertThat(painel.situacoes()).containsEntry("CANCELADO", 1L);
        assertThat(consulta.atendimentosMes(Optional.of(vendedor)))
                .singleElement()
                .satisfies(a -> assertThat(a.nome()).startsWith("Cliente anonimizado"));
    }

    @Test
    @DisplayName("RG-08: as raízes publicam eventos específicos do histórico e da agenda")
    void eventosDoRegistro() {
        var contrato = contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        registrar(contrato, ResultadoContato.CONCLUIDO, null);
        assertThat(eventos.stream(AtendimentoRegistrado.class))
                .singleElement()
                .satisfies(evento -> assertThat(evento.dados().autor()).isEqualTo(vendedor));
        assertThat(eventos.stream(AcompanhamentoAtualizado.class))
                .singleElement()
                .satisfies(
                        evento -> {
                            assertThat(evento.anterior().etapa()).contains((short) 1);
                            assertThat(evento.novo().etapa()).contains((short) 2);
                        });
    }

    @Test
    @DisplayName("FIL-06 / RG-08: a referência publica o prazo anterior e o novo pelo agregado")
    void eventoDePrazo() {
        Map<Short, Integer> prazos = new LinkedHashMap<>();
        regua.etapas()
                .forEach(
                        etapa ->
                                prazos.put(
                                        etapa.id(),
                                        etapa.codigo().equals("D30")
                                                ? 25
                                                : etapa.diasAposAnterior()));
        var previa = configuracao.prever(prazos);
        configuracao.salvar(prazos, previa.confirmacao(), vendedor);
        assertThat(eventos.stream(PrazoDaEtapaAlterado.class))
                .singleElement()
                .satisfies(
                        evento -> {
                            assertThat(evento.anterior()).isEqualTo(23);
                            assertThat(evento.novo()).isEqualTo(25);
                            assertThat(evento.autor()).isEqualTo(vendedor);
                        });
    }

    @Test
    @DisplayName("FIL-04: a consulta real de fevereiro seleciona nascidos em 29/02 uma única vez")
    void fevereiroNaConsulta() {
        associado.corrigirDados(
                new DadosPessoais(
                        "José FIL", LocalDate.of(1992, 2, 29), associado.getMunicipioId()),
                vendedor);
        contrato(HOJE.minusDays(7), SituacaoContrato.ATIVO);
        em.flush();
        em.clear();
        assertThat(
                        consultaFilas.aniversarios(
                                new PeriodoDeAniversarios(LocalDate.of(2027, 2, 28), 0),
                                Optional.of(vendedor)))
                .hasSize(1);
        assertThat(
                        consultaFilas.aniversarios(
                                new PeriodoDeAniversarios(LocalDate.of(2028, 2, 28), 0),
                                Optional.of(vendedor)))
                .isEmpty();
    }

    private static String cpf() {
        var aleatorio = new Random();
        var cpf = new StringBuilder();
        for (int i = 0; i < 9; i++) cpf.append(aleatorio.nextInt(10));
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            int digito = 11 - soma % 11;
            cpf.append(digito >= 10 ? 0 : digito);
        }
        return cpf.toString();
    }
}
