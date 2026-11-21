package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.exportacao.FormatoExportacao;
import br.com.superauto.crm.relacionamento.entidade.Correcao;
import br.com.superauto.crm.relacionamento.entidade.Corretor;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDoHistorico;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.AutorDeAtendimento;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDoHistorico;
import br.com.superauto.crm.relacionamento.servico.AtendimentoAvulsoService;
import br.com.superauto.crm.relacionamento.servico.ConsultaHistoricoService;
import br.com.superauto.crm.relacionamento.servico.CorrecaoDeAtendimentoService;
import br.com.superauto.crm.relacionamento.servico.LinhaDoHistorico;
import br.com.superauto.crm.relacionamento.servico.OrigemDoRegistro;
import br.com.superauto.crm.relacionamento.servico.PreviaDoContato;
import br.com.superauto.crm.relacionamento.servico.RegistroAvulso;
import br.com.superauto.crm.relacionamento.servico.RegistroDeAtendimentoService;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ATD-01 a ATD-05 pela ficha e pelo histórico: registrar pós-venda e avulso, corrigir, consultar e exportar.
 * O vendedor só registra e vê os atendimentos da sua carteira (RG-03).
 */
@RestController
@RequestMapping("/api/interno/atendimentos")
@RequiredArgsConstructor
public class AtendimentosController {
    private final RegistroDeAtendimentoService registros;
    private final AtendimentoAvulsoService avulsos;
    private final CorrecaoDeAtendimentoService correcoes;
    private final ConsultaHistoricoService historico;
    private final Clock relogio;

    /** Filtros do histórico como chegam na URL; tudo opcional, o período padrão é o mês corrente. */
    public record FiltroHistoricoRequest(
            LocalDate inicio,
            LocalDate fim,
            TipoDeRelacionamento tipo,
            ResultadoContato resultado,
            UUID usuario,
            Integer cidade,
            String busca) {
        FiltroDoHistorico filtro() {
            return new FiltroDoHistorico(
                    Optional.ofNullable(tipo),
                    Optional.ofNullable(resultado),
                    Optional.ofNullable(usuario),
                    Optional.ofNullable(cidade),
                    Optional.ofNullable(busca));
        }

        PeriodoDoHistorico periodo(LocalDate hoje) {
            return PeriodoDoHistorico.de(Optional.ofNullable(inicio), Optional.ofNullable(fim), hoje);
        }
    }

    @GetMapping
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public List<LinhaDoHistorico> listar(@ModelAttribute FiltroHistoricoRequest req, JwtAuthenticationToken auth) {
        return historico.listar(req.periodo(hoje()), req.filtro(), UsuarioAtual.vendedorRestrito(auth));
    }

    @GetMapping("/autores")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public List<AutorDeAtendimento> autores(JwtAuthenticationToken auth) {
        return historico.autores(UsuarioAtual.vendedorRestrito(auth));
    }

    /** Regras da CAR-07: só gerencial e vendedor exportam, e o vendedor só a sua carteira. */
    @GetMapping("/exportacao")
    @PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
    public ResponseEntity<byte[]> exportar(
            @ModelAttribute FiltroHistoricoRequest req,
            @RequestParam(defaultValue = "XLSX") FormatoExportacao formato,
            JwtAuthenticationToken auth) {
        var linhas =
                historico.exportar(
                        req.periodo(hoje()), req.filtro(), AutorDaRequisicao.de(auth));
        return formato.anexo(ColunasDoHistorico.planilha(linhas), "atendimentos-" + hoje());
    }

    @PostMapping("/contratos/{id}/previa")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public PreviaDoContato prever(
            @PathVariable Long id,
            @RequestBody @Valid RegistroDeContatoRequest req,
            JwtAuthenticationToken auth) {
        return registros.prever(
                id, req.comando(OrigemDoRegistro.FICHA), AutorDaRequisicao.de(auth));
    }

    @PostMapping("/contratos/{id}/registro")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public void registrarPosVenda(
            @PathVariable Long id,
            @RequestBody @Valid RegistroDeContatoRequest req,
            JwtAuthenticationToken auth) {
        registros.registrar(
                id, req.comando(OrigemDoRegistro.FICHA), AutorDaRequisicao.de(auth));
    }

    public record AvulsoRequest(
            @NotNull(message = "Informe o cliente.") Long associadoId,
            Long contratoId,
            @NotNull(message = "Informe o tipo do atendimento.") TipoDeRelacionamento tipo,
            @NotNull(message = "Informe o resultado do contato.") ResultadoContato resultado,
            LocalDate retorno,
            @Size(max = 2000, message = "O resumo deve ter até 2.000 caracteres.") String descricao,
            LocalDateTime realizadoEm) {
        RegistroAvulso comando() {
            return new RegistroAvulso(
                    associadoId,
                    Optional.ofNullable(contratoId),
                    tipo,
                    resultado,
                    Optional.ofNullable(retorno),
                    descricao,
                    Optional.ofNullable(realizadoEm));
        }
    }

    @PostMapping("/avulsos")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public void registrarAvulso(@RequestBody @Valid AvulsoRequest req, JwtAuthenticationToken auth) {
        avulsos.registrar(req.comando(), AutorDaRequisicao.de(auth));
    }

    public record CorrecaoRequest(
            @NotNull(message = "Informe o resultado correto.") ResultadoContato resultado,
            LocalDate retorno,
            @Size(max = 2000, message = "O resumo deve ter até 2.000 caracteres.") String descricao) {
        Correcao correcao() {
            return new Correcao(resultado, Optional.ofNullable(retorno), descricao);
        }
    }

    @PostMapping("/{id}/correcao")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public void corrigir(
            @PathVariable Long id, @RequestBody @Valid CorrecaoRequest req, JwtAuthenticationToken auth) {
        var corretor =
                new Corretor(UsuarioAtual.exigirId(auth), UsuarioAtual.temPapel(auth, Papeis.GERENCIAL));
        correcoes.corrigir(id, req.correcao(), corretor);
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
