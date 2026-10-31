package br.com.superauto.crm.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio único da aplicação no fuso da regional. Regras de "hoje"/"atrasado" devem injetar este
 * {@link Clock} (e não usar {@code LocalDate.now()} direto), o que também facilita os testes.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock(CrmProperties props) {
        return Clock.system(props.fusoHorario());
    }
}
