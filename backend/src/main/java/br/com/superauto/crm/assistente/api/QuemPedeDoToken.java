package br.com.superauto.crm.assistente.api;

import br.com.superauto.crm.assistente.servico.QuemPede;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Traduz o token em quem usa o assistente: o id e, para o vendedor, o escopo da carteira dele (RG-03). */
final class QuemPedeDoToken {

    private QuemPedeDoToken() {
    }

    static QuemPede de(JwtAuthenticationToken auth) {
        return new QuemPede(UsuarioAtual.exigirId(auth), UsuarioAtual.vendedorRestrito(auth));
    }
}
