package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.servico.ConfiguracaoReguaService;
import br.com.superauto.crm.relacionamento.servico.PreviaDaRegua;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gestao/regua")
@RequiredArgsConstructor
public class ConfiguracaoReguaController {
    private final ConfiguracaoReguaService configuracao;

    public record PrazosRequest(
            @NotEmpty(message = "Informe os prazos da régua.")
                    Map<
                                    Short,
                                    @NotNull(message = "Informe o prazo da etapa.")
                                    @Min(value = 1, message = "O prazo mínimo é 1 dia.")
                                    @Max(value = 365, message = "O prazo máximo é 365 dias.")
                                    Integer>
                            prazos,
            String confirmacao) {}

    @GetMapping
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public List<EtapaRegua> etapas() {
        return configuracao.etapas();
    }

    @PostMapping("/previa")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public PreviaDaRegua previa(@RequestBody @Valid PrazosRequest req) {
        return configuracao.prever(req.prazos());
    }

    @PutMapping
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public void salvar(@RequestBody @Valid PrazosRequest req, JwtAuthenticationToken auth) {
        configuracao.salvar(req.prazos(), req.confirmacao(), UsuarioAtual.exigirId(auth));
    }
}
