package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.relacionamento.servico.ContadoresFilasService;
import br.com.superauto.crm.relacionamento.servico.ContadoresFilasService.Contadores;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ACE-02 · Quantidades das filas "Contatos de hoje" e "Atrasados", exibidas ao lado dos itens do menu.
 * A URL mantém {@code /menu} porque é o contrato já usado pelo front.
 */
@RestController
@RequestMapping("/api/interno/menu")
public class ContadoresFilasController {

    private final ContadoresFilasService contadores;

    public ContadoresFilasController(ContadoresFilasService contadores) {
        this.contadores = contadores;
    }

    @GetMapping("/contadores")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public Contadores contadores(JwtAuthenticationToken auth) {
        return contadores.contar(UsuarioAtual.vendedorRestrito(auth));
    }
}
