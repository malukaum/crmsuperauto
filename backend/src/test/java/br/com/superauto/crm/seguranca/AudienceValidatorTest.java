package br.com.superauto.crm.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("crm-api");

    @Test
    void aceitaTokenComAudienciaDaApi() {
        assertThat(validator.validate(jwt(List.of("account", "crm-api"))).hasErrors()).isFalse();
    }

    @Test
    void recusaTokenDeOutraAudiencia() {
        var resultado = validator.validate(jwt(List.of("account")));
        assertThat(resultado.hasErrors()).isTrue();
        assertThat(resultado.getErrors()).first()
                .satisfies(e -> assertThat(e.getDescription()).contains("crm-api"));
    }

    @Test
    void recusaTokenSemAudiencia() {
        var semAud = Jwt.withTokenValue("t").header("alg", "RS256").subject("u")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        assertThat(validator.validate(semAud).hasErrors()).isTrue();
    }

    private static Jwt jwt(List<String> aud) {
        return Jwt.withTokenValue("t").header("alg", "RS256").subject("u").audience(aud)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}
