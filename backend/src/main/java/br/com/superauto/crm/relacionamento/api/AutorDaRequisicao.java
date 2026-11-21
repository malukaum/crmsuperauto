package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.relacionamento.servico.AutorDoContato;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Traduz o token em quem registra o contato: o id e, para o vendedor, o escopo da carteira dele (RG-03). */
final class AutorDaRequisicao {

    private AutorDaRequisicao() {
    }

    static AutorDoContato de(JwtAuthenticationToken auth) {
        return new AutorDoContato(UsuarioAtual.exigirId(auth), UsuarioAtual.vendedorRestrito(auth));
    }
}
