package br.com.superauto.crm.acesso.infraestrutura.keycloak;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.config.CrmProperties;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Camada anticorrupção: {@link ProvedorIdentidade} sobre a Admin REST API do Keycloak, autenticado pelo
 * {@link TokenDeServicoKeycloak}, com os erros traduzidos por {@link ErrosKeycloak}. A representação do
 * Keycloak não sai deste pacote: tudo vira {@link Conta}.
 */
@Component
public class KeycloakAdminClient implements ProvedorIdentidade {

    static final String ACAO_DEFINIR_SENHA = "UPDATE_PASSWORD";

    private static final ParameterizedTypeReference<List<UsuarioKeycloak>> LISTA_USUARIOS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<PapelKeycloak>> LISTA_PAPEIS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() { };

    private static final Duration TEMPO_CONEXAO = Duration.ofSeconds(5);
    private static final Duration TEMPO_RESPOSTA = Duration.ofSeconds(15);
    /** Quanto vale o link do e-mail para definir a senha. */
    private static final Duration VALIDADE_CONVITE = Duration.ofDays(3);
    /** Teto de usuários por papel numa listagem; a equipe da regional é bem menor. */
    private static final int MAXIMO_POR_PAPEL = 10_000;
    private static final String CLIENT_WEB = "crm-web";

    private final CrmProperties.KeycloakAdmin config;
    private final String frontendUrl;
    private final RestClient http;
    private final TokenDeServicoKeycloak tokens;
    private final Map<String, PapelKeycloak> papeis = new ConcurrentHashMap<>();

    public KeycloakAdminClient(CrmProperties props, Clock clock) {
        this.config = props.keycloakAdmin();
        this.frontendUrl = props.frontendUrl();
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TEMPO_CONEXAO);
        fabrica.setReadTimeout(TEMPO_RESPOSTA);
        this.http = RestClient.builder().baseUrl(config.url()).requestFactory(fabrica).build();
        this.tokens = new TokenDeServicoKeycloak(http, config, clock);
    }

    /** Representação de usuário da Admin API. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record UsuarioKeycloak(String id, String username, String firstName, String lastName, String email,
                           Boolean enabled, Boolean emailVerified, List<String> requiredActions) {

        Conta paraConta() {
            return new Conta(UUID.fromString(id), username, firstName, lastName, Email.seValido(email).orElse(null),
                    Boolean.TRUE.equals(enabled),
                    requiredActions != null && requiredActions.contains(ACAO_DEFINIR_SENHA));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PapelKeycloak(String id, String name) {
    }


    // ---------- ProvedorIdentidade ----------

    @Override
    public List<Conta> contasComPapel(String papel) {
        List<UsuarioKeycloak> lista = chamar(() -> admin(HttpMethod.GET,
                "/roles/{papel}/users?first=0&max={maximo}&briefRepresentation=false", papel, MAXIMO_POR_PAPEL)
                .retrieve().body(LISTA_USUARIOS));
        return lista == null ? List.of() : lista.stream().map(UsuarioKeycloak::paraConta).toList();
    }

    @Override
    public Optional<Conta> buscar(UUID id) {
        UsuarioKeycloak u = chamar(() -> {
            try {
                return admin(HttpMethod.GET, "/users/{id}", id).retrieve().body(UsuarioKeycloak.class);
            } catch (HttpClientErrorException.NotFound e) {
                return null;
            }
        });
        return Optional.ofNullable(u).map(UsuarioKeycloak::paraConta);
    }

    @Override
    public List<String> papeis(UUID id) {
        List<PapelKeycloak> lista = chamar(() -> admin(HttpMethod.GET, "/users/{id}/role-mappings/realm", id)
                .retrieve().body(LISTA_PAPEIS));
        return lista == null ? List.of() : lista.stream().map(PapelKeycloak::name).toList();
    }

    @Override
    public UUID criar(NovaConta nova) {
        var rep = new UsuarioKeycloak(null, nova.username(), nova.nome(), nova.sobrenome(), nova.email().valor(),
                true, false, List.of(ACAO_DEFINIR_SENHA));
        URI local = chamar(() -> admin(HttpMethod.POST, "/users")
                .contentType(MediaType.APPLICATION_JSON).body(rep)
                .retrieve().toBodilessEntity().getHeaders().getLocation());
        if (local == null) {
            throw new IntegracaoIndisponivelException("O Keycloak não devolveu o id do usuário criado.", null);
        }
        String caminho = local.getPath();
        return UUID.fromString(caminho.substring(caminho.lastIndexOf('/') + 1));
    }

    /** Lê a representação completa e devolve com só o "enabled" alterado (o PUT substitui o usuário inteiro). */
    @Override
    public void definirAtivo(UUID id, boolean ativo) {
        Map<String, Object> rep = new HashMap<>(Objects.requireNonNull(
                chamar(() -> admin(HttpMethod.GET, "/users/{id}", id).retrieve().body(MAPA))));
        rep.put("enabled", ativo);
        chamar(() -> admin(HttpMethod.PUT, "/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).body(rep).retrieve().toBodilessEntity());
    }

    @Override
    public void encerrarSessoes(UUID id) {
        chamar(() -> admin(HttpMethod.POST, "/users/{id}/logout", id).retrieve().toBodilessEntity());
    }

    @Override
    public void adicionarPapeis(UUID id, Collection<String> nomes) {
        alterarPapeis(HttpMethod.POST, id, nomes);
    }

    @Override
    public void removerPapeis(UUID id, Collection<String> nomes) {
        alterarPapeis(HttpMethod.DELETE, id, nomes);
    }

    /** O link do e-mail vale 3 dias e, ao final, leva a pessoa para o CRM. */
    @Override
    public void enviarConviteDefinirSenha(UUID id) {
        chamar(() -> admin(HttpMethod.PUT,
                        "/users/{id}/execute-actions-email?client_id={client}&redirect_uri={uri}&lifespan={segundos}",
                        id, CLIENT_WEB, frontendUrl + "/", VALIDADE_CONVITE.toSeconds())
                .contentType(MediaType.APPLICATION_JSON).body(List.of(ACAO_DEFINIR_SENHA))
                .retrieve().toBodilessEntity());
    }

    // ---------- infraestrutura ----------

    private void alterarPapeis(HttpMethod metodo, UUID id, Collection<String> nomes) {
        if (nomes.isEmpty()) {
            return;
        }
        var reps = nomes.stream().map(this::papel).toList();
        chamar(() -> admin(metodo, "/users/{id}/role-mappings/realm", id)
                .contentType(MediaType.APPLICATION_JSON).body(reps).retrieve().toBodilessEntity());
    }

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
                .headers(h -> h.setBearerAuth(tokens.valido()));
    }


    private <T> T chamar(Supplier<T> chamada) {
        return ErrosKeycloak.traduzir(chamada, tokens::invalidar);
    }
}
