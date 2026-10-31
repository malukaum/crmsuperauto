package br.com.superauto.crm.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import br.com.superauto.crm.config.ClockConfig;
import br.com.superauto.crm.config.CrmProperties;
import br.com.superauto.crm.config.SecurityConfig;
import br.com.superauto.crm.seguranca.KeycloakRealmRoleConverter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Matriz de acesso: cada endpoint × cada papel (e sem token), verificando 200 / 401 / 403.
 * O JWT é simulado com {@code jwt()}; os papéis passam pelo mesmo converter usado em produção.
 */
@WebMvcTest(controllers = {MeController.class, PingController.class})
@Import({SecurityConfig.class, ClockConfig.class})
@EnableConfigurationProperties(CrmProperties.class)
class SegurancaEndpointsTest {

    private static final String ANONIMO = "anonimo";
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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
                Arguments.of("/api/cliente/ping", "cliente", 200));
    }

    @ParameterizedTest(name = "{1} em {0} -> {2}")
    @MethodSource("matriz")
    void matrizDeAcesso(String endpoint, String papel, int esperado) throws Exception {
        var req = get(endpoint);
        if (!ANONIMO.equals(papel)) {
            req.with(tokenCom(papel));
        }
        var resultado = mvc.perform(req).andExpect(status().is(esperado));
        if (esperado == 401) {
            resultado.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Não autenticado"));
        } else if (esperado == 403) {
            resultado.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Acesso negado"))
                    .andExpect(jsonPath("$.instance").value(endpoint));
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
        String username = switch (papel) {
            case "gerencial" -> "gerente";
            default -> papel + "1";
        };
        return jwt()
                .jwt(j -> j.subject(username)
                        .claim("preferred_username", username)
                        .claim("aud", List.of("crm-api"))
                        .claim("realm_access", Map.of("roles", List.of(papel, "offline_access"))))
                .authorities(new KeycloakRealmRoleConverter());
    }
}
