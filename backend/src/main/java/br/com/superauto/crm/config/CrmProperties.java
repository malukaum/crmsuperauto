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
        Cors cors) {

    /**
     * @param issuer     valor esperado no claim {@code iss} (URL pública do realm)
     * @param jwkSetUri  onde buscar as chaves públicas; pode ser um endereço interno (ex.: rede do Docker)
     * @param audiencia  valor exigido no claim {@code aud}
     */
    public record Seguranca(String issuer, String jwkSetUri, @DefaultValue("crm-api") String audiencia) {
    }

    public record Cors(@DefaultValue List<String> origens) {
    }
}
