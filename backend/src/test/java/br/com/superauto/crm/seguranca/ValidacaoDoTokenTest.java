package br.com.superauto.crm.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

class ValidacaoDoTokenTest {
    private static final String EMISSOR = "http://localhost:8180/realms/crm";
    private static final String AUDIENCIA = "crm-api";

    private static Jwt token(Instant expiraEm) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(EMISSOR)
                .audience(List.of(AUDIENCIA))
                .issuedAt(expiraEm.minusSeconds(300))
                .expiresAt(expiraEm)
                .build();
    }

    @Test
    @DisplayName("ACE-03: token vencido (sessão parada, sem renovação) é recusado pela API")
    void tokenVencidoRecusado() {
        var resultado = ValidacaoDoToken.criar(EMISSOR, AUDIENCIA).validate(token(Instant.now().minusSeconds(120)));

        assertThat(resultado.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("ACE-03: token dentro da validade, do emissor e do público certos, é aceito")
    void tokenValidoAceito() {
        var resultado = ValidacaoDoToken.criar(EMISSOR, AUDIENCIA).validate(token(Instant.now().plusSeconds(240)));

        assertThat(resultado.hasErrors()).isFalse();
    }
}
