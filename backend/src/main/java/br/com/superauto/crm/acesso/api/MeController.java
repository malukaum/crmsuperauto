package br.com.superauto.crm.acesso.api;

import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UsuarioLogadoDto me(JwtAuthenticationToken auth) {
        var jwt = auth.getToken();
        List<String> papeis = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()).toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
        return new UsuarioLogadoDto(
                auth.getName(),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"),
                papeis,
                jwt.getClaimAsString("cpf"));
    }
}
