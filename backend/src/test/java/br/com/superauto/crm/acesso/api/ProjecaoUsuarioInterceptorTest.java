package br.com.superauto.crm.acesso.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.superauto.crm.acesso.servico.DadosProjecao;
import br.com.superauto.crm.acesso.servico.ProjecaoUsuarioService;
import br.com.superauto.crm.compartilhado.entidade.Email;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ProjecaoUsuarioInterceptorTest {

    private final ProjecaoUsuarioService projecao = mock(ProjecaoUsuarioService.class);
    private final ProjecaoUsuarioInterceptor interceptor = new ProjecaoUsuarioInterceptor(projecao);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Repassa id, username, nome e e-mail do token da equipe")
    void repassaClaims() {
        UUID id = UUID.randomUUID();
        autenticar(id.toString(), "ROLE_VENDEDOR");
        interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());
        verify(projecao).registrar(id, new DadosProjecao("vendedor1", "Vendedor Um", Email.de("vendedor1@x.com")));
    }

    @Test
    @DisplayName("Sub que não é UUID é ignorado")
    void subInvalido() {
        autenticar("nao-uuid", "ROLE_VENDEDOR");
        interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());
        verify(projecao, never()).registrar(any(UUID.class), any(DadosProjecao.class));
    }

    private static void autenticar(String sub, String papel) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject(sub)
                .claim("preferred_username", "vendedor1").claim("name", "Vendedor Um").claim("email", "vendedor1@x.com")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(papel))));
    }
}
