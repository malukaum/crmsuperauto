package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.relacionamento.entidade.AjusteDaRegua;
import br.com.superauto.crm.relacionamento.servico.AjusteDaReguaService;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

/** ATD-04 · O gerencial ajusta a régua de um contrato depois de um atendimento lançado errado. */
@RestController
@RequestMapping("/api/gestao/regua/contratos")
@RequiredArgsConstructor
public class ReguaDoContratoController {
    private final AjusteDaReguaService ajustes;

    public record AjusteRequest(
            @NotNull(message = "Informe a versão do acompanhamento.")
                    @Min(value = 0, message = "A versão não pode ser negativa.")
                    Integer versao,
            @NotNull(message = "Escolha a etapa pendente.") Short etapaId,
            LocalDate retorno) {
        AjusteDaRegua ajuste() {
            return new AjusteDaRegua(versao, etapaId, Optional.ofNullable(retorno));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public void ajustar(
            @PathVariable Long id, @RequestBody @Valid AjusteRequest req, JwtAuthenticationToken auth) {
        ajustes.ajustar(id, req.ajuste(), UsuarioAtual.exigirId(auth));
    }
}
