package br.com.superauto.crm.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void converteTodosOsPapeisDoCrm() {
        var jwt = jwtCom(Map.of("realm_access", Map.of("roles",
                List.of("gerencial", "vendedor", "atendente", "cliente"))));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_GERENCIAL", "ROLE_VENDEDOR", "ROLE_ATENDENTE", "ROLE_CLIENTE");
    }

    @Test
    void ignoraPapeisPadraoDoKeycloak() {
        var jwt = jwtCom(Map.of("realm_access", Map.of("roles",
                List.of("default-roles-crm", "offline_access", "uma_authorization", "vendedor"))));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_VENDEDOR");
    }

    @Test
    void semRealmAccessNaoTemAuthorities() {
        assertThat(converter.convert(jwtCom(Map.of("outro", "valor")))).isEmpty();
    }

    @Test
    void realmAccessSemRolesNaoTemAuthorities() {
        assertThat(converter.convert(jwtCom(Map.of("realm_access", Map.of())))).isEmpty();
    }

    static Jwt jwtCom(Map<String, Object> claims) {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("usuario")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(c -> c.putAll(claims))
                .build();
    }
}
