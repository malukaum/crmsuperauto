package br.com.superauto.crm.relacionamento.infraestrutura;

import br.com.superauto.crm.relacionamento.servico.IndicadoresDaCarteira;
import br.com.superauto.crm.relacionamento.servico.SelecaoDeTrabalho;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Serviços de domínio puros, mantidos independentes do framework. */
@Configuration
public class RelacionamentoConfig {
    @Bean
    SelecaoDeTrabalho selecaoDeTrabalho() {
        return new SelecaoDeTrabalho();
    }

    @Bean
    IndicadoresDaCarteira indicadoresDaCarteira() {
        return new IndicadoresDaCarteira();
    }
}
