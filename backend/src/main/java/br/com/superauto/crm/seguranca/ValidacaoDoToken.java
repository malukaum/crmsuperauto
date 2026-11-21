package br.com.superauto.crm.seguranca;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.time.Duration;

/**
 * Regras que a API aplica a todo token do Keycloak: emissor do realm, público da API e validade.
 *
 * <p>ACE-03 · A validade é o que faz a inatividade valer também no servidor: o token de acesso dura 5 minutos e
 * só é renovado quando a pessoa usa o CRM. Sem uso por 30 minutos, o Keycloak encerra a sessão (não renova mais)
 * e a API recusa o token vencido com 401.
 */
public final class ValidacaoDoToken {

    /** Diferença de relógio tolerada entre o Keycloak e a API. */
    static final Duration TOLERANCIA_DE_RELOGIO = Duration.ofSeconds(30);

    private ValidacaoDoToken() {
    }

    public static OAuth2TokenValidator<Jwt> criar(String emissor, String audiencia) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(emissor),
                new JwtTimestampValidator(TOLERANCIA_DE_RELOGIO),
                new AudienceValidator(audiencia));
    }
}
