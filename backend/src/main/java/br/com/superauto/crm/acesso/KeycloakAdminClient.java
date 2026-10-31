package br.com.superauto.crm.acesso;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import br.com.superauto.crm.config.CrmProperties;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;
import br.com.superauto.crm.erro.NegocioException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente mínimo da Admin REST API do Keycloak, autenticado pela conta de serviço do client da API
 * (grant client_credentials). Só o que a gestão de usuários da equipe precisa.
 */
@Component
public class KeycloakAdminClient {

    private static final ParameterizedTypeReference<List<UsuarioKeycloak>> LISTA_USUARIOS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<PapelKeycloak>> LISTA_PAPEIS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() { };

    private final CrmProperties.KeycloakAdmin config;
    private final String frontendUrl;
    private final Clock clock;
    private final RestClient http;
    private final Map<String, PapelKeycloak> papeis = new ConcurrentHashMap<>();

    private volatile String token;
    private volatile Instant tokenExpiraEm = Instant.EPOCH;

    public KeycloakAdminClient(CrmProperties props, Clock clock) {
        this.config = props.keycloakAdmin();
        this.frontendUrl = props.frontendUrl();
        this.clock = clock;
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder().baseUrl(config.url()).requestFactory(fabrica).build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UsuarioKeycloak(String id, String username, String firstName, String lastName, String email,
                                  Boolean enabled, Boolean emailVerified, List<String> requiredActions) {

        public String nomeCompleto() {
            String nome = (Objects.requireNonNullElse(firstName, "") + " " + Objects.requireNonNullElse(lastName, "")).trim();
            return nome.isEmpty() ? username : nome;
        }

        public boolean ativo() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PapelKeycloak(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResposta(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }

    // ---------- usuários ----------

    public List<UsuarioKeycloak> usuariosComPapel(String papel) {
        return chamar(() -> admin(HttpMethod.GET, "/roles/{papel}/users?first=0&max=10000&briefRepresentation=false", papel)
                .retrieve().body(LISTA_USUARIOS));
    }

    public UsuarioKeycloak buscar(String id) {
        return chamar(() -> admin(HttpMethod.GET, "/users/{id}", id).retrieve().body(UsuarioKeycloak.class));
    }

    public List<String> papeisDoRealm(String id) {
        List<PapelKeycloak> lista = chamar(() -> admin(HttpMethod.GET, "/users/{id}/role-mappings/realm", id)
                .retrieve().body(LISTA_PAPEIS));
        return lista == null ? List.of() : lista.stream().map(PapelKeycloak::name).toList();
    }

    /** Cria o usuário e devolve o id gerado pelo Keycloak. */
    public String criar(UsuarioKeycloak novo) {
        URI local = chamar(() -> admin(HttpMethod.POST, "/users")
                .contentType(MediaType.APPLICATION_JSON).body(novo)
                .retrieve().toBodilessEntity().getHeaders().getLocation());
        if (local == null) {
            throw new IntegracaoIndisponivelException("O Keycloak não devolveu o id do usuário criado.", null);
        }
        String caminho = local.getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    /** Liga ou desliga o login. Lê a representação completa e devolve com só o "enabled" alterado. */
    public void definirAtivo(String id, boolean ativo) {
        Map<String, Object> rep = new HashMap<>(Objects.requireNonNull(
                chamar(() -> admin(HttpMethod.GET, "/users/{id}", id).retrieve().body(MAPA))));
        rep.put("enabled", ativo);
        chamar(() -> admin(HttpMethod.PUT, "/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).body(rep).retrieve().toBodilessEntity());
    }

    /** Derruba todas as sessões do usuário (o próximo refresh de token falha e ele volta ao login). */
    public void encerrarSessoes(String id) {
        chamar(() -> admin(HttpMethod.POST, "/users/{id}/logout", id).retrieve().toBodilessEntity());
    }

    public void adicionarPapeis(String id, List<String> nomes) {
        if (nomes.isEmpty()) {
            return;
        }
        var reps = nomes.stream().map(this::papel).toList();
        chamar(() -> admin(HttpMethod.POST, "/users/{id}/role-mappings/realm", id)
                .contentType(MediaType.APPLICATION_JSON).body(reps).retrieve().toBodilessEntity());
    }

    public void removerPapeis(String id, List<String> nomes) {
        if (nomes.isEmpty()) {
            return;
        }
        var reps = nomes.stream().map(this::papel).toList();
        chamar(() -> admin(HttpMethod.DELETE, "/users/{id}/role-mappings/realm", id)
                .contentType(MediaType.APPLICATION_JSON).body(reps).retrieve().toBodilessEntity());
    }

    /**
     * Envia o e-mail do Keycloak com o link para definir a senha. O link vale 3 dias e,
     * ao final, leva a pessoa para o CRM.
     */
    public void enviarEmailDefinirSenha(String id) {
        chamar(() -> admin(HttpMethod.PUT,
                        "/users/{id}/execute-actions-email?client_id=crm-web&redirect_uri={uri}&lifespan={segundos}",
                        id, frontendUrl + "/", Duration.ofDays(3).toSeconds())
                .contentType(MediaType.APPLICATION_JSON).body(List.of("UPDATE_PASSWORD"))
                .retrieve().toBodilessEntity());
    }

    // ---------- infraestrutura ----------

    private PapelKeycloak papel(String nome) {
        return papeis.computeIfAbsent(nome, n -> chamar(() -> admin(HttpMethod.GET, "/roles/{nome}", n)
                .retrieve().body(PapelKeycloak.class)));
    }

    private RestClient.RequestBodySpec admin(HttpMethod metodo, String caminho, Object... variaveis) {
        Object[] todas = new Object[variaveis.length + 1];
        todas[0] = config.realm();
        System.arraycopy(variaveis, 0, todas, 1, variaveis.length);
        return http.method(metodo)
                .uri("/admin/realms/{realm}" + caminho, todas)
                .headers(h -> h.setBearerAuth(tokenDeServico()));
    }

    private String tokenDeServico() {
        Instant agora = clock.instant();
        if (token != null && agora.isBefore(tokenExpiraEm)) {
            return token;
        }
        synchronized (this) {
            if (token == null || !agora.isBefore(tokenExpiraEm)) {
                var form = new LinkedMultiValueMap<String, String>();
                form.add("grant_type", "client_credentials");
                form.add("client_id", config.clientId());
                form.add("client_secret", config.clientSecret());
                TokenResposta resposta = chamar(() -> http.post()
                        .uri("/realms/{realm}/protocol/openid-connect/token", config.realm())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                        .retrieve().body(TokenResposta.class));
                if (resposta == null || resposta.accessToken() == null) {
                    throw new IntegracaoIndisponivelException("O Keycloak não devolveu o token da conta de serviço.", null);
                }
                token = resposta.accessToken();
                // Renova 30 s antes de vencer.
                tokenExpiraEm = agora.plusSeconds(Math.max(resposta.expiresIn() - 30, 0));
            }
            return token;
        }
    }

    private <T> T chamar(java.util.function.Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw NegocioException.naoEncontrado("Usuário não encontrado.");
        } catch (HttpClientErrorException.Conflict e) {
            throw NegocioException.conflito("usuario-duplicado", "Já existe um usuário com este username ou e-mail.");
        } catch (HttpClientErrorException.Unauthorized e) {
            token = null;
            throw new IntegracaoIndisponivelException(
                    "O CRM não conseguiu se autenticar no Keycloak. Verifique a conta de serviço.", e);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && e.getResponseBodyAsString().contains("email")) {
                throw NegocioException.invalido("email-invalido", "O Keycloak recusou o e-mail informado.");
            }
            throw new IntegracaoIndisponivelException("O Keycloak recusou a operação (" + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            throw new IntegracaoIndisponivelException(
                    "Não foi possível falar com o Keycloak agora. Tente novamente em instantes.", e);
        }
    }
}
