package br.com.superauto.crm.seguranca;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Converte {@code realm_access.roles} do token do Keycloak em authorities {@code ROLE_*}.
 * Só os papéis conhecidos do CRM viram authority; papéis padrão do Keycloak
 * (offline_access, uma_authorization, default-roles-crm) são ignorados.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final List<String> PAPEIS_CONHECIDOS =
            List.of(Papeis.GERENCIAL, Papeis.VENDEDOR, Papeis.ATENDENTE, Papeis.CLIENTE);

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(PAPEIS_CONHECIDOS::contains)
                .distinct()
                .<GrantedAuthority>map(papel -> new SimpleGrantedAuthority("ROLE_" + papel.toUpperCase(Locale.ROOT)))
                .toList();
    }
}
