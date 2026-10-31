package br.com.superauto.crm.seguranca;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Leitura dos dados do usuário autenticado a partir do token. */
public final class UsuarioAtual {

    private UsuarioAtual() {
    }

    /** O {@code sub} do Keycloak, que é também o id da projeção local ({@code usuario.id}). */
    public static Optional<UUID> id(Jwt jwt) {
        try {
            return Optional.ofNullable(jwt.getSubject()).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<UUID> id(Authentication auth) {
        return auth instanceof JwtAuthenticationToken t ? id(t.getToken()) : Optional.empty();
    }

    public static boolean temPapel(Authentication auth, String papel) {
        String esperado = "ROLE_" + papel.toUpperCase(java.util.Locale.ROOT);
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> esperado.equals(a.getAuthority()));
    }

    public static boolean ehEquipeInterna(Authentication auth) {
        return Papeis.EQUIPE.stream().anyMatch(p -> temPapel(auth, p));
    }
}
