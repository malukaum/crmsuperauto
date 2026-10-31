package br.com.superauto.crm.config;

import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configurações da aplicação, lidas de {@code crm.*} (preenchidas por variáveis de ambiente nos profiles).
 */
@ConfigurationProperties(prefix = "crm")
public record CrmProperties(
        @DefaultValue("America/Sao_Paulo") ZoneId fusoHorario,
        Seguranca seguranca,
        Cors cors,
        KeycloakAdmin keycloakAdmin,
        @DefaultValue("http://localhost:4200") String frontendUrl) {

    /**
     * @param issuer     valor esperado no claim {@code iss} (URL pública do realm)
     * @param jwkSetUri  onde buscar as chaves públicas; pode ser um endereço interno (ex.: rede do Docker)
     * @param audiencia  valor exigido no claim {@code aud}
     */
    public record Seguranca(String issuer, String jwkSetUri, @DefaultValue("crm-api") String audiencia) {
    }

    public record Cors(@DefaultValue List<String> origens) {
    }

    /**
     * Acesso à Admin API do Keycloak (gestão de usuários da equipe), pela conta de serviço do client da API.
     *
     * @param url          endereço do servidor Keycloak visto pelo backend (pode ser interno, ex.: rede do Docker)
     * @param realm        realm do CRM
     * @param clientId     client confidencial com conta de serviço (papéis manage-users e view-users)
     * @param clientSecret segredo desse client
     */
    public record KeycloakAdmin(@DefaultValue("http://localhost:8180") String url,
                                @DefaultValue("crm") String realm,
                                @DefaultValue("crm-api") String clientId,
                                String clientSecret) {
    }
}
