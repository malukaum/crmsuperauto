package br.com.superauto.crm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.superauto.crm.assistente.api.AnotacoesController;
import br.com.superauto.crm.assistente.api.ConfiguracaoIaController;
import br.com.superauto.crm.assistente.api.RecomendacoesController;
import br.com.superauto.crm.assistente.api.TendenciasController;
import br.com.superauto.crm.assistente.entidade.SituacaoDoResumo;
import br.com.superauto.crm.assistente.servico.AnotacaoService;
import br.com.superauto.crm.assistente.servico.ConfiguracaoDeIaService;
import br.com.superauto.crm.assistente.servico.RecomendacoesService;
import br.com.superauto.crm.assistente.servico.ResumoDaAnotacao;
import br.com.superauto.crm.assistente.servico.TendenciasService;
import br.com.superauto.crm.acesso.api.EquipeController;
import br.com.superauto.crm.acesso.api.MeController;
import br.com.superauto.crm.acesso.api.PingController;
import br.com.superauto.crm.acesso.api.UsuariosEquipeController;
import br.com.superauto.crm.acesso.servico.ConsultaEquipeService;
import br.com.superauto.crm.acesso.servico.DadosProjecao;
import br.com.superauto.crm.acesso.servico.GestaoUsuariosService;
import br.com.superauto.crm.acesso.servico.ProjecaoUsuarioService;
import br.com.superauto.crm.cadastro.api.AssociadosController;
import br.com.superauto.crm.cadastro.api.CarteiraController;
import br.com.superauto.crm.cadastro.api.ContratosController;
import br.com.superauto.crm.cadastro.api.GestaoAssociadosController;
import br.com.superauto.crm.cadastro.api.GestaoContratosController;
import br.com.superauto.crm.cadastro.servico.CadastroContratoService;
import br.com.superauto.crm.cadastro.servico.ConsultaAssociadoService;
import br.com.superauto.crm.cadastro.servico.ConsultaCarteiraService;
import br.com.superauto.crm.cadastro.servico.EscopoCarteira;
import br.com.superauto.crm.cadastro.servico.ExportacaoCarteiraService;
import br.com.superauto.crm.cadastro.servico.GestaoAssociadoService;
import br.com.superauto.crm.cadastro.servico.GestaoContratosService;
import br.com.superauto.crm.config.ClockConfig;
import br.com.superauto.crm.config.CrmProperties;
import br.com.superauto.crm.config.SecurityConfig;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.referencia.api.ReferenciasController;
import br.com.superauto.crm.referencia.servico.ConsultaReferenciasService;
import br.com.superauto.crm.relacionamento.api.AtendimentosController;
import br.com.superauto.crm.relacionamento.api.ConfiguracaoReguaController;
import br.com.superauto.crm.relacionamento.api.ReguaDoContratoController;
import br.com.superauto.crm.relacionamento.servico.AjusteDaReguaService;
import br.com.superauto.crm.relacionamento.servico.AtendimentoAvulsoService;
import br.com.superauto.crm.relacionamento.servico.ConsultaHistoricoService;
import br.com.superauto.crm.relacionamento.servico.CorrecaoDeAtendimentoService;
import br.com.superauto.crm.relacionamento.api.ContadoresFilasController;
import br.com.superauto.crm.relacionamento.api.TrabalhoRelacionamentoController;
import br.com.superauto.crm.relacionamento.servico.ConfiguracaoReguaService;
import br.com.superauto.crm.relacionamento.servico.ConsultaTrabalhoService;
import br.com.superauto.crm.relacionamento.servico.ContadoresFilasService;
import br.com.superauto.crm.relacionamento.servico.ContadoresFilasService.Contadores;
import br.com.superauto.crm.relacionamento.servico.RegistroDeAtendimentoService;
import br.com.superauto.crm.seguranca.KeycloakRealmRoleConverter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Matriz de acesso: cada endpoint × cada papel (e sem token), verificando 200 / 401 / 403.
 * O JWT é simulado com {@code jwt()}; os papéis passam pelo mesmo converter usado em produção.
 */
@WebMvcTest(controllers = {MeController.class, PingController.class, ContadoresFilasController.class,
        UsuariosEquipeController.class, EquipeController.class, ReferenciasController.class, CarteiraController.class,
        AssociadosController.class, ContratosController.class, GestaoContratosController.class,
        GestaoAssociadosController.class, TrabalhoRelacionamentoController.class, ConfiguracaoReguaController.class,
        AtendimentosController.class, ReguaDoContratoController.class, AnotacoesController.class,
        TendenciasController.class, RecomendacoesController.class, ConfiguracaoIaController.class})
@Import({SecurityConfig.class, ClockConfig.class})
@EnableConfigurationProperties(CrmProperties.class)
class SegurancaEndpointsTest {

    @MockitoBean private ConsultaTrabalhoService consultaTrabalho;
    @MockitoBean private RegistroDeAtendimentoService registroNaFila;
    @MockitoBean private ConfiguracaoReguaService configuracaoRegua;
    @MockitoBean private AtendimentoAvulsoService atendimentoAvulso;
    @MockitoBean private CorrecaoDeAtendimentoService correcaoDeAtendimento;
    @MockitoBean private ConsultaHistoricoService consultaHistorico;
    @MockitoBean private AjusteDaReguaService ajusteDaRegua;
    @MockitoBean private AnotacaoService anotacoes;
    @MockitoBean private TendenciasService tendencias;
    @MockitoBean private RecomendacoesService recomendacoes;
    @MockitoBean private ConfiguracaoDeIaService configuracaoIa;

    private static final String ANONIMO = "anonimo";
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ProjecaoUsuarioService projecao;

    @MockitoBean
    private GestaoUsuariosService gestaoUsuarios;

    @MockitoBean
    private ConsultaEquipeService consultaEquipe;

    @MockitoBean
    private ContadoresFilasService contadoresFilas;

    @MockitoBean
    private ConsultaReferenciasService referencias;

    @MockitoBean
    private ConsultaCarteiraService consultaCarteira;

    @MockitoBean
    private ExportacaoCarteiraService exportacaoCarteira;

    @MockitoBean
    private ConsultaAssociadoService consultaAssociado;

    @MockitoBean
    private GestaoAssociadoService gestaoAssociado;

    @MockitoBean
    private CadastroContratoService cadastroContrato;

    @MockitoBean
    private GestaoContratosService gestaoContratos;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(consultaTrabalho.contatos(any(), any())).thenReturn(Page.empty());
        when(consultaTrabalho.aniversarios(anyInt(), any(), any())).thenReturn(Page.empty());
        when(consultaHistorico.listar(any(), any(), any())).thenReturn(Page.empty());
        when(anotacoes.doAtendimento(any(), any())).thenReturn(Optional.of(new ResumoDaAnotacao(5L,
                SituacaoDoResumo.AGUARDANDO, "texto", List.of(), null, List.of(), null, List.of(), List.of(), false,
                null, null, null, null, null, null)));
    }

    static Stream<Arguments> matriz() {
        // endpoint, papel, status esperado
        return Stream.of(
                Arguments.of("/api/me", ANONIMO, 401),
                Arguments.of("/api/me", "gerencial", 200),
                Arguments.of("/api/me", "vendedor", 200),
                Arguments.of("/api/me", "atendente", 200),
                Arguments.of("/api/me", "cliente", 200),

                Arguments.of("/api/interno/ping", ANONIMO, 401),
                Arguments.of("/api/interno/ping", "gerencial", 200),
                Arguments.of("/api/interno/ping", "vendedor", 200),
                Arguments.of("/api/interno/ping", "atendente", 200),
                Arguments.of("/api/interno/ping", "cliente", 403),

                Arguments.of("/api/gestao/ping", ANONIMO, 401),
                Arguments.of("/api/gestao/ping", "gerencial", 200),
                Arguments.of("/api/gestao/ping", "vendedor", 403),
                Arguments.of("/api/gestao/ping", "atendente", 403),
                Arguments.of("/api/gestao/ping", "cliente", 403),

                Arguments.of("/api/cliente/ping", ANONIMO, 401),
                Arguments.of("/api/cliente/ping", "gerencial", 403),
                Arguments.of("/api/cliente/ping", "vendedor", 403),
                Arguments.of("/api/cliente/ping", "atendente", 403),
                Arguments.of("/api/cliente/ping", "cliente", 200),

                // ACE-02: contadores do menu, só para a equipe
                Arguments.of("/api/interno/menu/contadores", ANONIMO, 401),
                Arguments.of("/api/interno/menu/contadores", "gerencial", 200),
                Arguments.of("/api/interno/menu/contadores", "vendedor", 200),
                Arguments.of("/api/interno/menu/contadores", "atendente", 200),
                Arguments.of("/api/interno/menu/contadores", "cliente", 403),

                // ACE-04: gestão de usuários, só gerencial
                Arguments.of("/api/gestao/usuarios", ANONIMO, 401),
                Arguments.of("/api/gestao/usuarios", "gerencial", 200),
                Arguments.of("/api/gestao/usuarios", "vendedor", 403),
                Arguments.of("/api/gestao/usuarios", "atendente", 403),
                Arguments.of("/api/gestao/usuarios", "cliente", 403),

                // Vendedores para filtro e escolha: gerencial e atendente (o vendedor não usa o filtro)
                Arguments.of("/api/interno/equipe/vendedores", ANONIMO, 401),
                Arguments.of("/api/interno/equipe/vendedores", "gerencial", 200),
                Arguments.of("/api/interno/equipe/vendedores", "vendedor", 403),
                Arguments.of("/api/interno/equipe/vendedores", "atendente", 200),
                Arguments.of("/api/interno/equipe/vendedores", "cliente", 403),

                // Listas de apoio: toda a equipe
                Arguments.of("/api/interno/referencias/tipos-veiculo", ANONIMO, 401),
                Arguments.of("/api/interno/referencias/tipos-veiculo", "atendente", 200),
                Arguments.of("/api/interno/referencias/tipos-veiculo", "cliente", 403),
                Arguments.of("/api/interno/referencias/cidades?nome=formiga", "vendedor", 200),
                Arguments.of("/api/interno/referencias/etapas", "gerencial", 200),

                // CAR-01: carteira para toda a equipe
                Arguments.of("/api/interno/carteira", ANONIMO, 401),
                Arguments.of("/api/interno/carteira", "gerencial", 200),
                Arguments.of("/api/interno/carteira", "vendedor", 200),
                Arguments.of("/api/interno/carteira", "atendente", 200),
                Arguments.of("/api/interno/carteira", "cliente", 403),

                // CAR-07: exportação só gerencial e vendedor (proposta)
                Arguments.of("/api/interno/carteira/exportacao", ANONIMO, 401),
                Arguments.of("/api/interno/carteira/exportacao", "gerencial", 200),
                Arguments.of("/api/interno/carteira/exportacao", "vendedor", 200),
                Arguments.of("/api/interno/carteira/exportacao", "atendente", 403),
                Arguments.of("/api/interno/carteira/exportacao", "cliente", 403),

                // CAR-03: ficha para toda a equipe
                Arguments.of("/api/interno/associados/7", ANONIMO, 401),
                Arguments.of("/api/interno/associados/7", "gerencial", 200),
                Arguments.of("/api/interno/associados/7", "vendedor", 200),
                Arguments.of("/api/interno/associados/7", "atendente", 200),
                Arguments.of("/api/interno/associados/7", "cliente", 403),

                // RG-04: documento completo só para gerencial e vendedor
                Arguments.of("/api/interno/associados/7/documento", ANONIMO, 401),
                Arguments.of("/api/interno/associados/7/documento", "gerencial", 200),
                Arguments.of("/api/interno/associados/7/documento", "vendedor", 200),
                Arguments.of("/api/interno/associados/7/documento", "atendente", 403),
                Arguments.of("/api/interno/associados/7/documento", "cliente", 403),

                // CAR-02: busca pelo documento no início do cadastro, gerencial e vendedor
                Arguments.of("/api/interno/associados?documento=52998224725", "atendente", 403),
                Arguments.of("/api/interno/associados?documento=52998224725", "cliente", 403));
    }

    /** Comandos: método, endpoint, corpo, papel e status esperado. */
    static Stream<Arguments> comandos() {
        String contato = "{\"versao\":0,\"tipo\":\"WHATSAPP\",\"valor\":\"37998692950\",\"principal\":true,\"aceitaContato\":true}";
        String classificacao = "{\"versao\":0,\"perfil\":\"PROMOTOR\",\"potencial\":\"ALTO\"}";
        String dados = "{\"versao\":0,\"nome\":\"José\",\"municipioId\":1}";
        String alteracao = "{\"versao\":0,\"situacao\":\"ATIVO\",\"mensalidade\":139.90,\"vendedorId\":\""
                + UUID.randomUUID() + "\"}";
        String lote = "{\"contratoIds\":[1,2]}";
        String anonimizacao = "{\"versao\":0,\"confirmacao\":\"ANONIMIZAR\"}";
        return Stream.of(
                // CAR-02: cadastrar contrato, gerencial e vendedor
                Arguments.of("POST", "/api/interno/contratos", NOVO_CONTRATO, "vendedor", 201),
                Arguments.of("POST", "/api/interno/contratos", NOVO_CONTRATO, "gerencial", 201),
                Arguments.of("POST", "/api/interno/contratos", NOVO_CONTRATO, "atendente", 403),
                Arguments.of("POST", "/api/interno/contratos", NOVO_CONTRATO, "cliente", 403),
                Arguments.of("POST", "/api/interno/contratos", NOVO_CONTRATO, ANONIMO, 401),
                // CAR-04: contatos, toda a equipe (é o único dado cadastral que o atendente altera)
                Arguments.of("POST", "/api/interno/associados/7/contatos", contato, "atendente", 204),
                Arguments.of("PUT", "/api/interno/associados/7/contatos/3", contato, "vendedor", 204),
                Arguments.of("DELETE", "/api/interno/associados/7/contatos/3?versao=0", null, "gerencial", 204),
                Arguments.of("POST", "/api/interno/associados/7/contatos", contato, "cliente", 403),
                // CAR-06: classificação, toda a equipe
                Arguments.of("PUT", "/api/interno/associados/7/classificacao", classificacao, "atendente", 204),
                Arguments.of("PUT", "/api/interno/associados/7/classificacao", classificacao, "cliente", 403),
                // Dados pessoais: gerencial e vendedor
                Arguments.of("PUT", "/api/interno/associados/7/dados-pessoais", dados, "vendedor", 204),
                Arguments.of("PUT", "/api/interno/associados/7/dados-pessoais", dados, "atendente", 403),
                // CAR-05: só o gerencial
                Arguments.of("PUT", "/api/gestao/contratos/10", alteracao, "gerencial", 204),
                Arguments.of("PUT", "/api/gestao/contratos/10", alteracao, "vendedor", 403),
                Arguments.of("PUT", "/api/gestao/contratos/10", alteracao, "atendente", 403),
                Arguments.of("POST", "/api/gestao/contratos/ativacao-em-lote", lote, "gerencial", 200),
                Arguments.of("POST", "/api/gestao/contratos/ativacao-em-lote", lote, "vendedor", 403),
                // CAR-08: só o gerencial
                Arguments.of("POST", "/api/gestao/associados/7/anonimizacao", anonimizacao, "gerencial", 204),
                Arguments.of("POST", "/api/gestao/associados/7/anonimizacao", anonimizacao, "vendedor", 403),
                Arguments.of("POST", "/api/gestao/associados/7/anonimizacao", anonimizacao, "atendente", 403));
    }

    static Stream<Arguments> endpointsFilas() {
        var equipe = List.of("gerencial", "vendedor", "atendente");
        var endpoints =
                List.of(
                        new String[] {"GET", "/api/interno/painel", ""},
                        new String[] {"GET", "/api/interno/filas/hoje", ""},
                        new String[] {"GET", "/api/interno/filas/atrasados", ""},
                        new String[] {"GET", "/api/interno/aniversariantes", ""},
                        new String[] {"GET", "/api/interno/atendimentos", ""},
                        new String[] {
                            "POST",
                            "/api/interno/filas/contratos/1/previa",
                            "{\"versao\":0,\"resultado\":\"CONCLUIDO\"}"
                        },
                        new String[] {
                            "POST",
                            "/api/interno/filas/contratos/1/registro",
                            "{\"versao\":0,\"resultado\":\"CONCLUIDO\"}"
                        },
                        new String[] {
                            "POST",
                            "/api/interno/aniversariantes/contratos/1/registro",
                            ""
                        },
                        new String[] {"GET", "/api/gestao/regua", ""},
                        new String[] {"POST", "/api/gestao/regua/previa", "{\"prazos\":{\"1\":7}}"},
                        new String[] {
                            "PUT",
                            "/api/gestao/regua",
                            "{\"prazos\":{\"1\":7},\"confirmacao\":\"previa\"}"
                        });
        return endpoints.stream()
                .flatMap(
                        e ->
                                Stream.of(
                                                ANONIMO,
                                                "gerencial",
                                                "vendedor",
                                                "atendente",
                                                "cliente",
                                                "semperfil")
                                        .map(
                                                papel ->
                                                        Arguments.of(
                                                                e[0],
                                                                e[1],
                                                                e[2],
                                                                papel,
                                                                ANONIMO.equals(papel)
                                                                        ? 401
                                                                        : (e[1].startsWith(
                                                                                                "/api/gestao")
                                                                                        ? "gerencial"
                                                                                                .equals(
                                                                                                        papel)
                                                                                        : equipe
                                                                                                .contains(
                                                                                                        papel))
                                                                                ? 200
                                                                                : 403)));
    }

    @ParameterizedTest(name = "FIL: {3} em {0} {1} -> {4}")
    @MethodSource("endpointsFilas")
    void matrizDasFilas(String metodo, String endpoint, String corpo, String papel, int esperado)
            throws Exception {
        var req =
                request(HttpMethod.valueOf(metodo), endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo);
        if (!ANONIMO.equals(papel)) req.with(tokenCom(papel, UUID.randomUUID()));
        mvc.perform(req).andExpect(status().is(esperado));
    }

    static Stream<Arguments> endpointsAtendimentos() {
        var equipe = List.of("gerencial", "vendedor", "atendente");
        String posVenda = "{\"versao\":0,\"resultado\":\"CONCLUIDO\"}";
        String avulso = "{\"associadoId\":7,\"tipo\":\"SUPORTE\",\"resultado\":\"CONCLUIDO\"}";
        String correcao = "{\"resultado\":\"SEM_RESPOSTA\"}";
        String ajuste = "{\"versao\":0,\"etapaId\":2}";
        // ATD-01 a ATD-05: toda a equipe; exportação só gerencial e vendedor (CAR-07); ajuste da régua só gerencial
        var endpoints = List.of(
                new Object[] {"GET", "/api/interno/atendimentos", "", equipe},
                new Object[] {"GET", "/api/interno/atendimentos/autores", "", equipe},
                new Object[] {"GET", "/api/interno/atendimentos/exportacao", "", List.of("gerencial", "vendedor")},
                new Object[] {"POST", "/api/interno/atendimentos/contratos/1/previa", posVenda, equipe},
                new Object[] {"POST", "/api/interno/atendimentos/contratos/1/registro", posVenda, equipe},
                new Object[] {"POST", "/api/interno/atendimentos/avulsos", avulso, equipe},
                new Object[] {"POST", "/api/interno/atendimentos/5/correcao", correcao, equipe},
                new Object[] {"PUT", "/api/gestao/regua/contratos/1", ajuste, List.of("gerencial")});
        return endpoints.stream().flatMap(e -> Stream.of(ANONIMO, "gerencial", "vendedor", "atendente", "cliente")
                .map(papel -> Arguments.of(e[0], e[1], e[2], papel,
                        ANONIMO.equals(papel) ? 401 : ((List<?>) e[3]).contains(papel) ? 200 : 403)));
    }

    @ParameterizedTest(name = "ATD: {3} em {0} {1} -> {4}")
    @MethodSource("endpointsAtendimentos")
    void matrizDosAtendimentos(String metodo, String endpoint, String corpo, String papel, int esperado)
            throws Exception {
        var req = request(HttpMethod.valueOf(metodo), endpoint).contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (!ANONIMO.equals(papel)) req.with(tokenCom(papel, UUID.randomUUID()));
        mvc.perform(req).andExpect(status().is(esperado));
    }

    static Stream<Arguments> endpointsAssistente() {
        var equipe = List.of("gerencial", "vendedor", "atendente");
        var gerencialEVendedor = List.of("gerencial", "vendedor");
        var gerencial = List.of("gerencial");
        String provedor = "{\"versao\":0,\"ativo\":false,\"limiteMensal\":20}";
        String rota = "{\"versao\":0,\"principal\":\"CLAUDE\"}";
        // IA-01 a IA-05: anotar e sugerir para toda a equipe; tendências para gerencial e vendedor; configuração só gerencial
        var endpoints = List.of(
                new Object[] {"MULTIPART", "/api/interno/atendimentos/5/anotacao", "", equipe},
                new Object[] {"GET", "/api/interno/atendimentos/5/anotacao", "", equipe},
                new Object[] {"GET", "/api/interno/associados/7/anotacoes", "", equipe},
                new Object[] {"POST", "/api/interno/associados/7/recomendacoes", "", equipe},
                new Object[] {"GET", "/api/interno/assistente/tendencias", "", gerencialEVendedor},
                new Object[] {"POST", "/api/interno/assistente/tendencias/leitura", "", gerencialEVendedor},
                new Object[] {"GET", "/api/gestao/ia/provedores", "", gerencial},
                new Object[] {"PUT", "/api/gestao/ia/provedores/GEMINI", provedor, gerencial},
                new Object[] {"POST", "/api/gestao/ia/provedores/GEMINI/teste", "{}", gerencial},
                new Object[] {"GET", "/api/gestao/ia/tarefas", "", gerencial},
                new Object[] {"PUT", "/api/gestao/ia/tarefas/RESUMO", rota, gerencial},
                new Object[] {"GET", "/api/gestao/ia/uso", "", gerencial});
        return endpoints.stream().flatMap(e -> Stream.of(ANONIMO, "gerencial", "vendedor", "atendente", "cliente")
                .map(papel -> Arguments.of(e[0], e[1], e[2], papel,
                        ANONIMO.equals(papel) ? 401 : ((List<?>) e[3]).contains(papel) ? 200 : 403)));
    }

    @ParameterizedTest(name = "IA: {3} em {0} {1} -> {4}")
    @MethodSource("endpointsAssistente")
    void matrizDoAssistente(String metodo, String endpoint, String corpo, String papel, int esperado) throws Exception {
        var req = "MULTIPART".equals(metodo)
                ? multipart(endpoint).param("texto", "Cliente quer indicar o irmão")
                : request(HttpMethod.valueOf(metodo), endpoint).contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (!ANONIMO.equals(papel)) req.with(tokenCom(papel, UUID.randomUUID()));
        mvc.perform(req).andExpect(status().is(esperado));
    }

    @Test
    @DisplayName("ATD-03 / RG-06: gravação simultânea barrada pelo índice único responde 409 registro-duplicado")
    void registroDuplicado() throws Exception {
        var unicidade = new java.sql.SQLException("duplicate key", "23505");
        Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicado", unicidade))
                .when(registroNaFila).aniversario(eq(1L), any());

        mvc.perform(post("/api/interno/aniversariantes/contratos/1/registro")
                        .with(tokenCom("atendente", UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("registro-duplicado"));
    }

    @Test
    @DisplayName("ATD-05 / RG-03: o histórico recebe o escopo do vendedor; gerencial vê todos")
    void historicoNoEscopoDoVendedor() throws Exception {
        UUID vendedor = UUID.randomUUID();
        mvc.perform(get("/api/interno/atendimentos").with(tokenCom("vendedor", vendedor))).andExpect(status().isOk());
        mvc.perform(get("/api/interno/atendimentos").with(tokenCom("gerencial", UUID.randomUUID())))
                .andExpect(status().isOk());
        verify(consultaHistorico).listar(any(), eq(Optional.of(vendedor)), any());
        verify(consultaHistorico).listar(any(), eq(Optional.empty()), any());
    }

    @Test
    @DisplayName("Tabelas paginadas no servidor: ?pagina e ?tamanho viram o Pageable do Spring, com no máximo 100 por página")
    void paginacaoNoServidor() throws Exception {
        mvc.perform(get("/api/interno/atendimentos?pagina=2&tamanho=10").with(tokenCom("gerencial", UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/interno/filas/hoje?tamanho=500").with(tokenCom("gerencial", UUID.randomUUID())))
                .andExpect(status().isOk());

        var historico = ArgumentCaptor.forClass(Pageable.class);
        verify(consultaHistorico).listar(any(), any(), historico.capture());
        assertThat(historico.getValue().getPageNumber()).isEqualTo(2);
        assertThat(historico.getValue().getPageSize()).isEqualTo(10);
        var fila = ArgumentCaptor.forClass(Pageable.class);
        verify(consultaTrabalho).contatos(any(), fila.capture());
        assertThat(fila.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("ATD-02: atendimento avulso sem tipo é recusado com mensagem em português")
    void avulsoSemTipo() throws Exception {
        mvc.perform(post("/api/interno/atendimentos/avulsos").with(tokenCom("atendente", UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":7,\"resultado\":\"CONCLUIDO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").value("Informe o tipo do atendimento."));
    }

    @Test
    @DisplayName("FIL / RG-03: painel, filas e aniversários recebem o escopo do vendedor")
    void filasNoEscopoDoVendedor() throws Exception {
        UUID vendedor = UUID.randomUUID();
        mvc.perform(get("/api/interno/painel").with(tokenCom("vendedor", vendedor)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/interno/filas/hoje").with(tokenCom("vendedor", vendedor)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/interno/aniversariantes").with(tokenCom("vendedor", vendedor)))
                .andExpect(status().isOk());
        verify(consultaTrabalho).painel(Optional.of(vendedor));
        verify(consultaTrabalho).contatos(
                argThat(fila -> !fila.atrasados() && fila.vendedor().equals(Optional.of(vendedor))), any());
        verify(consultaTrabalho).aniversarios(eq(0), eq(Optional.of(vendedor)), any());
    }

    private static final String NOVO_CONTRATO = """
            {"documento":"529.982.247-25",
             "pessoa":{"nome":"José","whatsapp":"(37) 99869-2950","municipioId":1},
             "veiculo":{"placa":"haa-7216","modelo":"Onix","tipoVeiculoId":1},
             "contrato":{"mensalidade":124.82,"dataAdesao":"2026-03-13"}}
            """;

    @ParameterizedTest(name = "{3} em {0} {1} -> {4}")
    @MethodSource("comandos")
    void matrizDeComandos(String metodo, String endpoint, String corpo, String papel, int esperado) throws Exception {
        var req = request(HttpMethod.valueOf(metodo), endpoint).contentType(MediaType.APPLICATION_JSON);
        if (corpo != null) {
            req.content(corpo);
        }
        if (!ANONIMO.equals(papel)) {
            req.with(tokenCom(papel, UUID.randomUUID()));
        }
        mvc.perform(req).andExpect(status().is(esperado));
    }

    @Test
    @DisplayName("RG-03: o vendedor consulta a carteira só no próprio escopo, mesmo pedindo outro vendedor")
    void carteiraNoEscopoDoVendedor() throws Exception {
        UUID vendedor = UUID.randomUUID();

        mvc.perform(get("/api/interno/carteira?vendedor=" + UUID.randomUUID()).with(tokenCom("vendedor", vendedor)))
                .andExpect(status().isOk());

        verify(consultaCarteira).buscar(any(), any(), eq(new EscopoCarteira(Optional.of(vendedor))));
    }

    @Test
    @DisplayName("CAR-07: a exportação sai em Excel (.xlsx) por padrão, para download")
    void exportacaoEmExcel() throws Exception {
        mvc.perform(get("/api/interno/carteira/exportacao").with(tokenCom("gerencial", UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".xlsx")));
    }

    @Test
    @DisplayName("CAR-07: a exportação também sai em CSV quando pedida")
    void exportacaoEmCsv() throws Exception {
        mvc.perform(get("/api/interno/carteira/exportacao?formato=CSV").with(tokenCom("vendedor", UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".csv")));
    }

    @Test
    @DisplayName("CAR-01: página fora de 10, 25 ou 50 é recusada com código")
    void paginaInvalida() throws Exception {
        when(consultaCarteira.buscar(any(), any(), any())).thenCallRealMethod();

        mvc.perform(get("/api/interno/carteira?tamanho=30").with(tokenCom("gerencial", UUID.randomUUID())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("pagina-invalida"));
    }

    @Test
    @DisplayName("CAR-02: placa inválida no cadastro sai como 422 com o código do erro")
    void placaInvalida() throws Exception {
        mvc.perform(post("/api/interno/contratos").with(tokenCom("vendedor", UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOVO_CONTRATO.replace("haa-7216", "HA-12")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("placa-invalida"));
    }

    @Test
    @DisplayName("CAR-08: sem digitar ANONIMIZAR a anonimização é recusada")
    void anonimizacaoExigeConfirmacao() throws Exception {
        mvc.perform(post("/api/gestao/associados/7/anonimizacao").with(tokenCom("gerencial", UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versao\":0,\"confirmacao\":\"anonimizar\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.confirmacao").value("Digite ANONIMIZAR para confirmar."));
    }

    @Test
    @DisplayName("RG-06: gravação recusada pela trava otimista sai como 409 com a mensagem da regra")
    void edicaoSimultanea() throws Exception {
        Mockito.doThrow(new ObjectOptimisticLockingFailureException("Associado", 7L))
                .when(gestaoAssociado).classificar(any(), any(), any());

        mvc.perform(put("/api/interno/associados/7/classificacao").with(tokenCom("atendente", UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versao\":0,\"perfil\":\"PROMOTOR\",\"potencial\":\"ALTO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("edicao-simultanea"))
                .andExpect(jsonPath("$.detail").value(
                        "Este cadastro foi alterado por outra pessoa. Recarregue para ver a versão atual."));
    }

    @ParameterizedTest(name = "{1} em {0} -> {2}")
    @MethodSource("matriz")
    void matrizDeAcesso(String endpoint, String papel, int esperado) throws Exception {
        var req = get(endpoint);
        if (!ANONIMO.equals(papel)) {
            req.with(tokenCom(papel, UUID.randomUUID()));
        }
        var resultado = mvc.perform(req).andExpect(status().is(esperado));
        if (esperado == 401) {
            resultado.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Não autenticado"));
        } else if (esperado == 403) {
            resultado.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Acesso negado"))
                    .andExpect(jsonPath("$.instance").value(endpoint.split("\\?")[0]));
        }
    }

    @Test
    @DisplayName("/api/me devolve username, nome, e-mail, papéis e cpf do token")
    void meDevolveDadosDoToken() throws Exception {
        mvc.perform(get("/api/me").with(jwt()
                        .jwt(j -> j.subject("cliente1")
                                .claim("preferred_username", "cliente1")
                                .claim("name", "Cliente Um")
                                .claim("email", "cliente1@superauto.local")
                                .claim("cpf", "12345678909")
                                .claim("realm_access", Map.of("roles",
                                        List.of("cliente", "offline_access", "default-roles-crm"))))
                        .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("cliente1"))
                .andExpect(jsonPath("$.nome").value("Cliente Um"))
                .andExpect(jsonPath("$.email").value("cliente1@superauto.local"))
                .andExpect(jsonPath("$.cpf").value("12345678909"))
                .andExpect(jsonPath("$.papeis.length()").value(1))
                .andExpect(jsonPath("$.papeis[0]").value("cliente"));
    }

    @Test
    @DisplayName("Token sem nenhum papel do CRM é autenticado, mas recebe 403 nas áreas restritas")
    void tokenSemPapel() throws Exception {
        var semPapel = jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of("offline_access"))))
                .authorities(new KeycloakRealmRoleConverter());
        mvc.perform(get("/api/me").with(semPapel)).andExpect(status().isOk());
        mvc.perform(get("/api/interno/ping").with(semPapel)).andExpect(status().isForbidden());
        mvc.perform(get("/api/gestao/ping").with(semPapel)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cliente/ping").with(semPapel)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Vendedor só conta os contratos dele; gerencial e atendente contam a carteira toda")
    void contadoresDoMenuRespeitamOEscopo() throws Exception {
        when(contadoresFilas.contar(any())).thenReturn(new Contadores(3, 2));
        UUID vendedor = UUID.randomUUID();

        mvc.perform(get("/api/interno/menu/contadores").with(tokenCom("vendedor", vendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contatosHoje").value(3))
                .andExpect(jsonPath("$.atrasados").value(2));
        verify(contadoresFilas).contar(Optional.of(vendedor));

        mvc.perform(get("/api/interno/menu/contadores").with(tokenCom("atendente", UUID.randomUUID())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/interno/menu/contadores").with(tokenCom("gerencial", UUID.randomUUID())))
                .andExpect(status().isOk());
        verify(contadoresFilas, times(2)).contar(Optional.empty());
    }

    @Test
    @DisplayName("Toda chamada da equipe garante a projeção local do usuário; cliente não")
    void projecaoLocalSoParaEquipe() throws Exception {
        mvc.perform(get("/api/interno/ping").with(tokenCom("atendente", UUID.randomUUID()))).andExpect(status().isOk());
        verify(projecao, times(1)).registrar(any(UUID.class), any(DadosProjecao.class));

        mvc.perform(get("/api/cliente/ping").with(tokenCom("cliente", UUID.randomUUID()))).andExpect(status().isOk());
        verify(projecao, times(1)).registrar(any(UUID.class), any(DadosProjecao.class));
    }

    @Test
    @DisplayName("Criar usuário valida os campos e responde em português")
    void criarUsuarioValidaCampos() throws Exception {
        mvc.perform(post("/api/gestao/usuarios").with(tokenCom("gerencial", UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"nome\":\"\",\"sobrenome\":\"Silva\",\"email\":\"x\",\"papel\":\"vendedor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.campos.nome").value("Informe o nome."))
                .andExpect(jsonPath("$.campos.email").value("Informe um e-mail válido."))
                .andExpect(jsonPath("$.campos.username").exists());
    }

    @Test
    @DisplayName("Regra de negócio sai como ProblemDetail com código e dados extras")
    void erroDeNegocio() throws Exception {
        UUID alvo = UUID.randomUUID();
        when(gestaoUsuarios.desativar(eq(alvo), any(), any())).thenThrow(new NegocioException(NegocioException.Tipo.CONFLITO,
                "contratos-a-transferir", "Transfira os contratos.", Map.of("contratosVigentes", 4L)));

        mvc.perform(post("/api/gestao/usuarios/" + alvo + "/desativar").with(tokenCom("gerencial", UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.codigo").value("contratos-a-transferir"))
                .andExpect(jsonPath("$.contratosVigentes").value(4));
    }

    @Test
    @DisplayName("Bearer malformado recebe 401 em ProblemDetail")
    void bearerMalformado() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer isto-nao-e-um-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.detail").value("Token de acesso inválido ou expirado. Faça login novamente."));
    }

    @Test
    @DisplayName("Preflight CORS do front em dev é aceito")
    void preflightCors() throws Exception {
        mvc.perform(options("/api/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    @DisplayName("Origem não autorizada é recusada no CORS")
    void corsOrigemDesconhecida() throws Exception {
        mvc.perform(options("/api/me")
                        .header("Origin", "http://site-estranho.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor tokenCom(String papel) {
        return tokenCom(papel, null);
    }

    private static RequestPostProcessor tokenCom(String papel, UUID sub) {
        String username = switch (papel) {
            case "gerencial" -> "gerente";
            default -> papel + "1";
        };
        return jwt()
                .jwt(j -> j.subject(sub == null ? username : sub.toString())
                        .claim("preferred_username", username)
                        .claim("aud", List.of("crm-api"))
                        .claim("realm_access", Map.of("roles", List.of(papel, "offline_access"))))
                .authorities(new KeycloakRealmRoleConverter());
    }
}
