package br.com.superauto.crm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClockConfigTest {

    @EnableConfigurationProperties(CrmProperties.class)
    static class Props {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Props.class, ClockConfig.class);

    @Test
    void clockPadraoNoFusoDeSaoPaulo() {
        runner.run(ctx -> assertThat(ctx.getBean(Clock.class).getZone())
                .isEqualTo(ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    void fusoConfiguravel() {
        runner.withPropertyValues("crm.fuso-horario=UTC")
                .run(ctx -> assertThat(ctx.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("UTC")));
    }
}
