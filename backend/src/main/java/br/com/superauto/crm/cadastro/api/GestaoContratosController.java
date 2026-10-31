package br.com.superauto.crm.cadastro.api;

import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.servico.AlteracaoContrato;
import br.com.superauto.crm.cadastro.servico.GestaoContratosService;
import br.com.superauto.crm.cadastro.servico.ResultadoAtivacao;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CAR-05 · Alterar situação, mensalidade, vendedor e código do SGA do contrato. Só o gerencial. */
@RestController
@RequestMapping("/api/gestao/contratos")
@PreAuthorize(Papeis.SO_GERENCIAL)
public class GestaoContratosController {

    /** Limite do lote por chamada, para uma confirmação não travar o banco. */
    private static final int MAXIMO_POR_LOTE = 500;

    private final GestaoContratosService gestao;

    public GestaoContratosController(GestaoContratosService gestao) {
        this.gestao = gestao;
    }

    public record AlteracaoContratoRequest(
            int versao,
            @NotNull(message = "Escolha a situação.") SituacaoContrato situacao,
            @NotNull(message = "Informe a mensalidade.")
            @DecimalMin(value = "0.00", message = "A mensalidade não pode ser negativa.")
            @Digits(integer = 10, fraction = 2, message = "A mensalidade aceita no máximo dois decimais.")
            BigDecimal mensalidade,
            @NotNull(message = "Escolha o vendedor.") UUID vendedorId,
            @Size(max = 30, message = "O código do SGA pode ter até 30 caracteres.") String codigoSga) {

        AlteracaoContrato alteracao() {
            return new AlteracaoContrato(versao, situacao, new Mensalidade(mensalidade), vendedorId,
                    Optional.ofNullable(codigoSga));
        }
    }

    public record AtivacaoEmLoteRequest(
            @NotEmpty(message = "Escolha ao menos um contrato.")
            @Size(max = MAXIMO_POR_LOTE, message = "Escolha até 500 contratos por vez.")
            List<@NotNull Long> contratoIds) {
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterar(@PathVariable Long id, @Valid @RequestBody AlteracaoContratoRequest req,
                        JwtAuthenticationToken auth) {
        gestao.alterar(id, req.alteracao(), UsuarioAtual.exigirId(auth));
    }

    /** Marca vários contratos "a conferir" como ativos de uma vez (proposta). */
    @PostMapping("/ativacao-em-lote")
    public ResultadoAtivacao ativarEmLote(@Valid @RequestBody AtivacaoEmLoteRequest req, JwtAuthenticationToken auth) {
        return gestao.ativarEmLote(req.contratoIds(), UsuarioAtual.exigirId(auth));
    }
}
