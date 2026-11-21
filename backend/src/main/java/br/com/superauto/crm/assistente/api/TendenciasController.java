package br.com.superauto.crm.assistente.api;

import br.com.superauto.crm.assistente.entidade.Tendencias;
import br.com.superauto.crm.assistente.servico.LeituraDasTendencias;
import br.com.superauto.crm.assistente.servico.PeriodoDasTendencias;
import br.com.superauto.crm.assistente.servico.TendenciasService;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/** IA-03 · Tendências da carteira (gerencial e vendedor; o vendedor vê só a carteira dele, RG-03). */
@RestController
@RequestMapping("/api/interno/assistente/tendencias")
@RequiredArgsConstructor
public class TendenciasController {
    private final TendenciasService tendencias;
    private final Clock relogio;

    @GetMapping
    @PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
    public Tendencias indicadores(@RequestParam(required = false) LocalDate inicio,
                                  @RequestParam(required = false) LocalDate fim, JwtAuthenticationToken auth) {
        return tendencias.indicadores(periodo(inicio, fim), UsuarioAtual.vendedorRestrito(auth));
    }

    @PostMapping("/leitura")
    @PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
    public LeituraDasTendencias leitura(@RequestParam(required = false) LocalDate inicio,
                                        @RequestParam(required = false) LocalDate fim, JwtAuthenticationToken auth) {
        return tendencias.leitura(periodo(inicio, fim), UsuarioAtual.vendedorRestrito(auth));
    }

    private PeriodoDasTendencias periodo(LocalDate inicio, LocalDate fim) {
        return PeriodoDasTendencias.de(Optional.ofNullable(inicio), Optional.ofNullable(fim), LocalDate.now(relogio));
    }
}
