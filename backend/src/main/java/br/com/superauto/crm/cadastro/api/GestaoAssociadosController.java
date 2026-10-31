package br.com.superauto.crm.cadastro.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import br.com.superauto.crm.cadastro.servico.FichaAberta;
import br.com.superauto.crm.cadastro.servico.GestaoAssociadoService;
import br.com.superauto.crm.seguranca.Papeis;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CAR-08 · Anonimizar cliente (LGPD). Só o gerencial. */
@RestController
@RequestMapping("/api/gestao/associados")
@PreAuthorize(Papeis.SO_GERENCIAL)
public class GestaoAssociadosController {

    private final GestaoAssociadoService gestao;

    public GestaoAssociadosController(GestaoAssociadoService gestao) {
        this.gestao = gestao;
    }

    /** É irreversível: a confirmação exige digitar ANONIMIZAR. */
    public record AnonimizacaoRequest(
            int versao,
            @NotNull(message = "Digite ANONIMIZAR para confirmar.")
            @Pattern(regexp = "ANONIMIZAR", message = "Digite ANONIMIZAR para confirmar.") String confirmacao) {
    }

    @PostMapping("/{id}/anonimizacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonimizar(@PathVariable Long id, @Valid @RequestBody AnonimizacaoRequest req,
                           JwtAuthenticationToken auth) {
        gestao.anonimizar(new FichaAberta(id, req.versao()), AutorDoToken.autor(auth));
    }
}
