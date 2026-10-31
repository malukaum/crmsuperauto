package br.com.superauto.crm.acesso.infraestrutura;

import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.acesso.servico.TransferenciaCarteira;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra no Spring os serviços de domínio do contexto Acesso, que não dependem do framework. */
@Configuration(proxyBeanMethods = false)
public class AcessoConfig {

    @Bean
    TransferenciaCarteira transferenciaCarteira(ProvedorIdentidade identidade) {
        return new TransferenciaCarteira(identidade);
    }
}
