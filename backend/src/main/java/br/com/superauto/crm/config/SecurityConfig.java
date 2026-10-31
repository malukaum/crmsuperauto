package br.com.superauto.crm.config;

import java.util.List;

import br.com.superauto.crm.seguranca.AudienceValidator;
import br.com.superauto.crm.seguranca.DelegarParaExceptionHandler;
import br.com.superauto.crm.seguranca.KeycloakRealmRoleConverter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        var erros = new DelegarParaExceptionHandler(resolver);

        http
            .cors(cors -> { })
            .csrf(csrf -> csrf.disable()) // API stateless com Bearer token; sem cookies de sessão
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(erros)
                .accessDeniedHandler(erros))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(erros)
                .accessDeniedHandler(erros));

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    /**
     * Decoder montado à mão para separar o issuer (o que vem no "iss", URL pública) de onde as chaves são
     * buscadas (pode ser a rede interna do Docker). Valida assinatura, expiração, issuer e audiência.
     * As chaves só são buscadas na primeira requisição autenticada, então a API sobe mesmo sem o Keycloak no ar.
     */
    @Bean
    JwtDecoder jwtDecoder(CrmProperties props) {
        var seg = props.seguranca();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(seg.jwkSetUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(seg.issuer()),
                new AudienceValidator(seg.audiencia())));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CrmProperties props) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(props.cors().origens());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
