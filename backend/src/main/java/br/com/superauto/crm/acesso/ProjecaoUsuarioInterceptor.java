package br.com.superauto.crm.acesso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes de cada chamada da API feita por alguém da equipe (gerencial, vendedor ou atendente),
 * garante a projeção local do usuário. Contas de cliente nascem pelo primeiro acesso, não por aqui.
 */
public class ProjecaoUsuarioInterceptor implements HandlerInterceptor {

    private final ProjecaoUsuarioService projecao;

    public ProjecaoUsuarioInterceptor(ProjecaoUsuarioService projecao) {
        this.projecao = projecao;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token && UsuarioAtual.ehEquipeInterna(auth)) {
            projecao.registrar(token.getToken());
        }
        return true;
    }
}
