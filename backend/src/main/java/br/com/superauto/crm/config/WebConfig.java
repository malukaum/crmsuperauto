package br.com.superauto.crm.config;

import br.com.superauto.crm.acesso.ProjecaoUsuarioInterceptor;
import br.com.superauto.crm.acesso.ProjecaoUsuarioService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final ProjecaoUsuarioService projecao;

    public WebConfig(ProjecaoUsuarioService projecao) {
        this.projecao = projecao;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ProjecaoUsuarioInterceptor(projecao)).addPathPatterns("/api/**");
    }
}
