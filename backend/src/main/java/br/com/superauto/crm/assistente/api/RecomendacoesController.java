package br.com.superauto.crm.assistente.api;

import br.com.superauto.crm.assistente.servico.RecomendacoesService;
import br.com.superauto.crm.assistente.servico.RecomendacoesService.Sugestoes;
import br.com.superauto.crm.seguranca.Papeis;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** IA-04 · Sugestões de abordagem para o próximo contato com o cliente (toda a equipe, no escopo de cada papel). */
@RestController
@RequestMapping("/api/interno/associados")
@RequiredArgsConstructor
public class RecomendacoesController {
    private final RecomendacoesService recomendacoes;

    @PostMapping("/{id}/recomendacoes")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public Sugestoes recomendar(@PathVariable Long id, JwtAuthenticationToken auth) {
        return recomendacoes.recomendar(id, QuemPedeDoToken.de(auth));
    }
}
