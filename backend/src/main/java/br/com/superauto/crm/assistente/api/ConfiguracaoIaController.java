package br.com.superauto.crm.assistente.api;

import br.com.superauto.crm.assistente.entidade.AjusteDoProvedor;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;
import br.com.superauto.crm.assistente.repositorio.UsoPorProvedor;
import br.com.superauto.crm.assistente.servico.AlteracaoDoProvedor;
import br.com.superauto.crm.assistente.servico.ConfiguracaoDeIaService;
import br.com.superauto.crm.assistente.servico.DefinicaoDaRota;
import br.com.superauto.crm.assistente.servico.ProvedorConfigurado;
import br.com.superauto.crm.assistente.servico.ResultadoDoTeste;
import br.com.superauto.crm.assistente.servico.RotaConfigurada;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** IA-05 · Configuração do assistente de IA, só para o gerencial. A chave entra por aqui e nunca sai. */
@RestController
@RequestMapping("/api/gestao/ia")
@RequiredArgsConstructor
public class ConfiguracaoIaController {
    private final ConfiguracaoDeIaService configuracao;

    public record ProvedorRequest(
            @NotNull(message = "Informe a versão.") @Min(value = 0, message = "A versão não pode ser negativa.") Integer versao,
            @Size(max = 100, message = "O modelo deve ter até 100 caracteres.") String modelo,
            @Size(max = 500, message = "A chave deve ter até 500 caracteres.") String chave,
            boolean ativo,
            @NotNull(message = "Informe o limite mensal.") @DecimalMin(value = "0", message = "O limite não pode ser negativo.") BigDecimal limiteMensal,
            @DecimalMin(value = "0", message = "O preço não pode ser negativo.") BigDecimal precoEntrada,
            @DecimalMin(value = "0", message = "O preço não pode ser negativo.") BigDecimal precoSaida) {
        AlteracaoDoProvedor alteracao() {
            return new AlteracaoDoProvedor(new AjusteDoProvedor(versao, modelo, ativo, limiteMensal,
                    Optional.ofNullable(precoEntrada), Optional.ofNullable(precoSaida)), Optional.ofNullable(chave));
        }

        @Override
        public String toString() {
            return "ProvedorRequest[modelo=" + modelo + ", ativo=" + ativo + ", chave=***]";
        }
    }

    public record TesteRequest(String modelo, String chave) {
        @Override
        public String toString() {
            return "TesteRequest[modelo=" + modelo + ", chave=***]";
        }
    }

    public record RotaRequest(
            @NotNull(message = "Informe a versão.") @Min(value = 0, message = "A versão não pode ser negativa.") Integer versao,
            @NotNull(message = "Escolha o provedor principal.") ProvedorDeIa principal,
            ProvedorDeIa reserva) {
        DefinicaoDaRota definicao() {
            return new DefinicaoDaRota(versao, principal, Optional.ofNullable(reserva));
        }
    }

    @GetMapping("/provedores")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public List<ProvedorConfigurado> provedores() {
        return configuracao.provedores();
    }

    @PutMapping("/provedores/{provedor}")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public ProvedorConfigurado ajustar(@PathVariable ProvedorDeIa provedor, @RequestBody @Valid ProvedorRequest req,
                                       JwtAuthenticationToken auth) {
        return configuracao.ajustar(provedor, req.alteracao(), UsuarioAtual.exigirId(auth));
    }

    @PostMapping("/provedores/{provedor}/teste")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public ResultadoDoTeste testar(@PathVariable ProvedorDeIa provedor, @RequestBody TesteRequest req) {
        return configuracao.testar(provedor, Optional.ofNullable(req.chave()), Optional.ofNullable(req.modelo()));
    }

    @GetMapping("/tarefas")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public List<RotaConfigurada> tarefas() {
        return configuracao.rotas();
    }

    @PutMapping("/tarefas/{tarefa}")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public RotaConfigurada definirTarefa(@PathVariable TarefaDeIa tarefa, @RequestBody @Valid RotaRequest req,
                                         JwtAuthenticationToken auth) {
        return configuracao.definirRota(tarefa, req.definicao(), UsuarioAtual.exigirId(auth));
    }

    @GetMapping("/uso")
    @PreAuthorize(Papeis.SO_GERENCIAL)
    public List<UsoPorProvedor> uso() {
        return configuracao.usoDoMes();
    }
}
