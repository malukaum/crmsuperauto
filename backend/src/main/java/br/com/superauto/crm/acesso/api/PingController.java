package br.com.superauto.crm.acesso.api;

import java.security.Principal;
import java.time.Clock;
import java.time.OffsetDateTime;

import br.com.superauto.crm.seguranca.Papeis;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de exemplo, só para provar o controle de acesso por papel de ponta a ponta.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    private final Clock clock;

    public PingController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/interno/ping")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public PingDto interno(Principal principal) {
        return ping("interno", "Acesso liberado para a equipe interna.", principal);
    }

    @GetMapping("/gestao/ping")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public PingDto gestao(Principal principal) {
        return ping("gestao", "Acesso liberado para a gestão.", principal);
    }

    @GetMapping("/cliente/ping")
    @PreAuthorize(Papeis.SO_CLIENTE)
    public PingDto cliente(Principal principal) {
        return ping("cliente", "Acesso liberado para a área do cliente.", principal);
    }

    private PingDto ping(String area, String mensagem, Principal principal) {
        return new PingDto(area, mensagem, principal.getName(), OffsetDateTime.now(clock));
    }
}
