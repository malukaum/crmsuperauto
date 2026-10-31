package br.com.superauto.crm.acesso.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import br.com.superauto.crm.acesso.servico.DadosProjecao;
import br.com.superauto.crm.acesso.servico.ProjecaoUsuarioService;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes de cada chamada da API feita por alguém da equipe (gerencial, vendedor ou atendente),
 * garante a projeção local do usuário a partir do token. Contas de cliente nascem pelo primeiro acesso,
 * não por aqui. Tokens sem {@code sub} UUID (ex.: testes) são ignorados.
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
            Jwt jwt = token.getToken();
            UsuarioAtual.id(jwt).ifPresent(id -> projecao.registrar(id, dadosDo(jwt)));
        }
        return true;
    }

    private static DadosProjecao dadosDo(Jwt jwt) {
        return new DadosProjecao(jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("name"),
                Email.seValido(jwt.getClaimAsString("email")).orElse(null));
    }
}
