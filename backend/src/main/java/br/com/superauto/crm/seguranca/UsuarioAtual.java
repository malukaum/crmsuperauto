package br.com.superauto.crm.seguranca;

import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.erro.NegocioException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Leitura dos dados do usuário autenticado a partir do token. */
public final class UsuarioAtual {

    /** Vendedor sem id válido no token não vê a carteira de ninguém. */
    private static final UUID NENHUM = new UUID(0L, 0L);

    private UsuarioAtual() {
    }

    /**
     * RG-03 · Gerencial e atendente veem toda a carteira (vazio); o vendedor, só os contratos em que é o vendedor
     * (o id dele).
     */
    public static Optional<UUID> vendedorRestrito(Authentication auth) {
        boolean veTudo = temPapel(auth, Papeis.GERENCIAL) || temPapel(auth, Papeis.ATENDENTE);
        return veTudo ? Optional.empty() : Optional.of(id(auth).orElse(NENHUM));
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

    /** O id de quem faz a operação; token sem {@code sub} válido não altera nada. */
    public static UUID exigirId(Authentication auth) {
        return id(auth).orElseThrow(() -> NegocioException.invalido("token-sem-id", "O token não identifica o usuário."));
    }

    public static boolean temPapel(Authentication auth, String papel) {
        String esperado = "ROLE_" + papel.toUpperCase(java.util.Locale.ROOT);
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> esperado.equals(a.getAuthority()));
    }

    public static boolean ehEquipeInterna(Authentication auth) {
        return Papeis.EQUIPE.stream().anyMatch(p -> temPapel(auth, p));
    }
}
