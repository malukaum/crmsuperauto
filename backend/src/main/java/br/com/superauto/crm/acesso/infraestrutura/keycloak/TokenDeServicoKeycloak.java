package br.com.superauto.crm.acesso.infraestrutura.keycloak;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import br.com.superauto.crm.config.CrmProperties;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Token da conta de serviço do client da API (grant client_credentials), guardado em memória e renovado
 * pouco antes de vencer. Seguro para uso concorrente.
 */
final class TokenDeServicoKeycloak {

    /** Renova antes de vencer, para não usar um token que expira no meio da chamada. */
    private static final Duration MARGEM_RENOVACAO = Duration.ofSeconds(30);

    private final RestClient http;
    private final CrmProperties.KeycloakAdmin config;
    private final Clock clock;

    private volatile String token;
    private volatile Instant expiraEm = Instant.EPOCH;

    TokenDeServicoKeycloak(RestClient http, CrmProperties.KeycloakAdmin config, Clock clock) {
        this.http = http;
        this.config = config;
        this.clock = clock;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResposta(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }

    String valido() {
        Instant agora = clock.instant();
        if (token != null && agora.isBefore(expiraEm)) {
            return token;
        }
        synchronized (this) {
            if (token == null || !agora.isBefore(expiraEm)) {
                renovar(agora);
            }
            return token;
        }
    }

    /** Descarta o token atual (ex.: o Keycloak o recusou); a próxima chamada pede outro. */
    void invalidar() {
        token = null;
    }

    private void renovar(Instant agora) {
        TokenResposta resposta = ErrosKeycloak.traduzir(this::pedirToken, this::invalidar);
        if (resposta == null || resposta.accessToken() == null) {
            throw new IntegracaoIndisponivelException("O Keycloak não devolveu o token da conta de serviço.", null);
        }
        token = resposta.accessToken();
        expiraEm = agora.plusSeconds(Math.max(resposta.expiresIn() - MARGEM_RENOVACAO.toSeconds(), 0));
    }

    private TokenResposta pedirToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", config.clientId());
        form.add("client_secret", config.clientSecret());
        return http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", config.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(TokenResposta.class);
    }
}
