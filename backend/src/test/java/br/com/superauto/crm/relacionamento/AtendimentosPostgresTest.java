package br.com.superauto.crm.relacionamento;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.acesso.entidade.Usuario;
import br.com.superauto.crm.cadastro.entidade.Adesao;
import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.NovoAssociado;
import br.com.superauto.crm.cadastro.entidade.NovoContrato;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.entidade.Veiculo;
import br.com.superauto.crm.cadastro.repositorio.HistoricoDeAtendimentos;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.AjusteDaRegua;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.Correcao;
import br.com.superauto.crm.relacionamento.entidade.Corretor;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDoHistorico;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDoHistorico;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;
import br.com.superauto.crm.relacionamento.servico.AjusteDaReguaService;
import br.com.superauto.crm.relacionamento.servico.AtendimentoAvulsoService;
import br.com.superauto.crm.relacionamento.servico.AutorDoContato;
import br.com.superauto.crm.relacionamento.servico.ConsultaHistoricoService;
import br.com.superauto.crm.relacionamento.servico.CorrecaoDeAtendimentoService;
import br.com.superauto.crm.relacionamento.servico.LinhaDoHistorico;
import br.com.superauto.crm.relacionamento.servico.OrigemDoRegistro;
import br.com.superauto.crm.relacionamento.servico.RegistroAvulso;
import br.com.superauto.crm.relacionamento.servico.RegistroDeAtendimentoService;
import br.com.superauto.crm.relacionamento.servico.RegistroDeContato;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/** ATD-01 a ATD-05 contra o Postgres local (opt-in, como as filas); cada cenário desfaz seus dados no fim. */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "FIL_TESTAR_BANCO", matches = "true")
class AtendimentosPostgresTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 31);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final UUID IMPORTACAO = new UUID(0L, 0L);
    private static final FiltroDoHistorico SEM_FILTROS = filtro(Optional.empty(), Optional.empty());

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
    @Autowired TiposDeContato tipos;
    @Autowired AtendimentoRepository atendimentos;
    @Autowired AcompanhamentoContratoRepository acompanhamentos;
    @Autowired RegistroDeAtendimentoService registros;
    @Autowired AtendimentoAvulsoService avulsos;
    @Autowired CorrecaoDeAtendimentoService correcoes;
    @Autowired AjusteDaReguaService ajustes;
    @Autowired ConsultaHistoricoService historico;
    @Autowired HistoricoDeAtendimentos historicoDaFicha;
    private UUID vendedor;
    private AutorDoContato autor;
    private Associado associado;
    private int numeroPlaca;
    private String ultimaPlaca;

    @BeforeEach
    void preparar() {
        vendedor = UUID.randomUUID();
        autor = new AutorDoContato(vendedor, Optional.of(vendedor));
        em.persist(new Usuario(vendedor, "teste-atd-" + vendedor, "Teste ATD", null));
        associado = Associado.cadastrar(
                new NovoAssociado(
                        Documento.de(cpf()),
                        new DadosPessoais("Maria Atendimento", LocalDate.of(1990, 5, 2), formiga()),
                        Telefone.de("37998692950"),
                        Optional.empty()),
                vendedor);
        em.persist(associado);
        numeroPlaca = new Random().nextInt(9000);
    }

    @Test
    @DisplayName("ATD-02: dado D+30 pendente, Suporte Concluído não muda etapa nem data")
    void avulsoNaoMexeNaRegua() {
        var contrato = contrato(SituacaoContrato.ATIVO);
        registrarPosVenda(contrato, ResultadoContato.CONCLUIDO, OrigemDoRegistro.FILA);
        var antes = acompanhamentos.findById(contrato.getId()).orElseThrow().estado();

        avulsos.registrar(avulso(TipoDeRelacionamento.SUPORTE, Optional.of(contrato.getId())), autor);
        em.flush();
        em.clear();

        assertThat(acompanhamentos.findById(contrato.getId()).orElseThrow().estado()).isEqualTo(antes);
        assertThat(antes.proximoContato()).contains(HOJE.plusDays(23));
    }

    @Test
    @DisplayName("ATD-04: no histórico aparecem os dois registros, o original marcado como corrigido")
    void correcaoNoHistorico() {
        var contrato = contrato(SituacaoContrato.ATIVO);
        registrarPosVenda(contrato, ResultadoContato.CONCLUIDO, OrigemDoRegistro.FILA);
        Long original = historicoDoMes(SEM_FILTROS).getFirst().id();

        correcoes.corrigir(original,
                new Correcao(ResultadoContato.SEM_RESPOSTA, Optional.empty(), "Era outro número"),
                new Corretor(vendedor, true));
        em.flush();
        em.clear();

        var linhas = historicoDoMes(SEM_FILTROS);
        assertThat(linhas).hasSize(2);
        assertThat(linhas).filteredOn(LinhaDoHistorico::corrigido).singleElement()
                .satisfies(l -> assertThat(l.id()).isEqualTo(original));
        assertThat(linhas).filteredOn(l -> original.equals(l.corrigeAtendimentoId())).singleElement()
                .satisfies(l -> assertThat(l.resultado()).isEqualTo(ResultadoContato.SEM_RESPOSTA));
        assertThat(historicoDaFicha.doAssociado(associado.getId()))
                .anySatisfy(a -> assertThat(a.corrigido()).isTrue());
    }

    @Test
    @DisplayName("ATD-04: depois de corrigir um Concluído por engano, o ajuste do gerencial volta ao D+7 da adesão")
    void ajusteDepoisDaCorrecao() {
        var contrato = contrato(SituacaoContrato.ATIVO);
        registrarPosVenda(contrato, ResultadoContato.CONCLUIDO, OrigemDoRegistro.FILA);
        correcoes.corrigir(historicoDoMes(SEM_FILTROS).getFirst().id(),
                new Correcao(ResultadoContato.SEM_RESPOSTA, Optional.empty(), null),
                new Corretor(vendedor, true));
        em.flush();
        em.clear();
        var acompanhamento = acompanhamentos.findById(contrato.getId()).orElseThrow();

        ajustes.ajustar(contrato.getId(),
                new AjusteDaRegua(acompanhamento.getVersao(), regua.etapas().getFirst().id(), Optional.empty()),
                vendedor);
        em.flush();
        em.clear();

        assertThat(acompanhamentos.findById(contrato.getId()).orElseThrow().getProximoContatoEm()).contains(HOJE);
    }

    @Test
    @DisplayName("ATD-05: atendimento importado de 21/10/2026 aparece em outubro com o autor \"Importação da planilha\"")
    void atendimentoImportado() {
        atendimentos.save(Atendimento.registrar(new Atendimento.Novo(associado.getId(), null,
                tipos.id(TipoDeRelacionamento.SUPORTE), null, ResultadoContato.CONCLUIDO, "TESTE", null,
                LocalDate.of(2026, 10, 21).atTime(9, 30).atZone(FUSO).toOffsetDateTime(), IMPORTACAO)));
        contrato(SituacaoContrato.ATIVO);
        em.flush();

        var outubro = historico.listar(
                new PeriodoDoHistorico(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
                filtro(Optional.empty(), Optional.of("maria atendimento")), Optional.empty());

        assertThat(outubro).singleElement().satisfies(l -> {
            assertThat(l.registradoPor()).isEqualTo("Importação da planilha");
            assertThat(l.descricao()).isEqualTo("TESTE");
            assertThat(l.proximaAcao()).isEqualTo("Sem alteração no retorno");
            assertThat(l.cidade()).isEqualTo("Formiga");
        });
    }

    @Test
    @DisplayName("ATD-05 / RG-03: filtro por tipo e escopo do vendedor; outro vendedor não vê nada")
    void filtrosEEscopo() {
        var contrato = contrato(SituacaoContrato.ATIVO);
        registrarPosVenda(contrato, ResultadoContato.CONCLUIDO, OrigemDoRegistro.FILA);
        avulsos.registrar(avulso(TipoDeRelacionamento.COMERCIAL, Optional.empty()), autor);
        em.flush();

        assertThat(historicoDoMes(filtro(Optional.of(TipoDeRelacionamento.COMERCIAL), Optional.empty())))
                .singleElement()
                .satisfies(l -> assertThat(l.tipoCodigo()).isEqualTo(TipoDeRelacionamento.COMERCIAL));
        assertThat(historicoDoMes(filtro(Optional.empty(), Optional.of(ultimaPlaca))))
                .singleElement()
                .satisfies(l -> assertThat(l.proximaAcao()).isEqualTo("Retorno automático"));
        assertThat(historico.listar(mes(), SEM_FILTROS, Optional.of(UUID.randomUUID()))).isEmpty();
        assertThat(historico.autores(Optional.of(vendedor))).extracting("nome").containsExactly("Teste ATD");
    }

    @Test
    @DisplayName("ATD-01: pela ficha, o contrato suspenso é registrado e a régua avança")
    void suspensoPelaFicha() {
        var contrato = contrato(SituacaoContrato.SUSPENSO);

        registrarPosVenda(contrato, ResultadoContato.CONCLUIDO, OrigemDoRegistro.FICHA);

        assertThat(acompanhamentos.findById(contrato.getId()).orElseThrow().getProximoContatoEm())
                .contains(HOJE.plusDays(23));
    }

    private void registrarPosVenda(Contrato contrato, ResultadoContato resultado, OrigemDoRegistro origem) {
        int versao = acompanhamentos.findById(contrato.getId()).orElseThrow().getVersao();
        registros.registrar(contrato.getId(),
                new RegistroDeContato(versao, resultado, Optional.empty(), null, Optional.empty(), origem), autor);
        em.flush();
        em.clear();
    }

    private List<LinhaDoHistorico> historicoDoMes(FiltroDoHistorico filtro) {
        return historico.listar(mes(), filtro, Optional.of(vendedor));
    }

    private static PeriodoDoHistorico mes() {
        return PeriodoDoHistorico.de(Optional.empty(), Optional.empty(), HOJE);
    }

    private static FiltroDoHistorico filtro(Optional<TipoDeRelacionamento> tipo, Optional<String> busca) {
        return new FiltroDoHistorico(tipo, Optional.empty(), Optional.empty(), Optional.empty(), busca);
    }

    private RegistroAvulso avulso(TipoDeRelacionamento tipo, Optional<Long> contrato) {
        return new RegistroAvulso(associado.getId(), contrato, tipo, ResultadoContato.CONCLUIDO, Optional.empty(),
                "Dúvida sobre o boleto", Optional.empty());
    }

    private Contrato contrato(SituacaoContrato situacao) {
        var adesao = HOJE.minusDays(7);
        ultimaPlaca = "ZZY" + String.format("%04d", numeroPlaca++);
        var veiculo = Veiculo.cadastrar(Placa.de(ultimaPlaca), "Veículo ATD", (short) 1);
        em.persist(veiculo);
        var contrato = Contrato.cadastrar(
                new NovoContrato(
                        new Adesao(associado.getId(), veiculo.getId(), Mensalidade.de("100.00"),
                                adesao.atStartOfDay(FUSO).toOffsetDateTime()),
                        vendedor, Optional.empty(), Optional.empty()),
                HOJE, vendedor);
        em.persist(contrato);
        contrato.alterarSituacao(situacao, HOJE.atStartOfDay(FUSO).toOffsetDateTime(), vendedor);
        em.persist(AcompanhamentoContrato.iniciar(contrato.getId(), regua.etapas(), adesao));
        em.flush();
        return contrato;
    }

    private Integer formiga() {
        return em.createQuery("select m.id from Municipio m where m.nome = :nome", Integer.class)
                .setParameter("nome", "Formiga")
                .getSingleResult();
    }

    private static String cpf() {
        var digitos = new int[11];
        var aleatorio = new Random();
        for (int i = 0; i < 9; i++) digitos[i] = aleatorio.nextInt(10);
        digitos[9] = verificador(digitos, 9);
        digitos[10] = verificador(digitos, 10);
        var texto = new StringBuilder();
        for (int d : digitos) texto.append(d);
        return texto.toString();
    }

    private static int verificador(int[] digitos, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += digitos[i] * (tamanho + 1 - i);
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
