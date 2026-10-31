package br.com.superauto.crm.cadastro.api;

import br.com.superauto.crm.cadastro.servico.Autor;
import br.com.superauto.crm.cadastro.servico.EscopoCarteira;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Traduz o token em quem pede e quanto da carteira essa pessoa enxerga (RG-03). */
final class AutorDoToken {

    private AutorDoToken() {
    }

    static EscopoCarteira escopo(JwtAuthenticationToken auth) {
        return new EscopoCarteira(UsuarioAtual.vendedorRestrito(auth));
    }

    static Autor autor(JwtAuthenticationToken auth) {
        return new Autor(UsuarioAtual.exigirId(auth), escopo(auth));
    }
}
