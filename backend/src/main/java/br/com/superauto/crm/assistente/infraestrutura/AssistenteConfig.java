package br.com.superauto.crm.assistente.infraestrutura;

import br.com.superauto.crm.assistente.servico.CalculoDeTendencias;
import br.com.superauto.crm.assistente.servico.Pseudonimizacao;
import br.com.superauto.crm.assistente.servico.SelecaoDeProvedores;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Serviços de domínio do assistente (sem Spring) e o agendamento do resumo em segundo plano (IA-01). */
@Configuration
@EnableScheduling
public class AssistenteConfig {

    @Bean
    Pseudonimizacao pseudonimizacao() {
        return new Pseudonimizacao();
    }

    @Bean
    CalculoDeTendencias calculoDeTendencias() {
        return new CalculoDeTendencias();
    }

    @Bean
    SelecaoDeProvedores selecaoDeProvedores() {
        return new SelecaoDeProvedores();
    }
}
