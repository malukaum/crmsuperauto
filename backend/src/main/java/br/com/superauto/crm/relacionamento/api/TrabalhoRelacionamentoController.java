package br.com.superauto.crm.relacionamento.api;

import org.springframework.data.web.PagedModel;
import org.springframework.data.domain.Pageable;
import br.com.superauto.crm.relacionamento.servico.FilaSolicitada;
import br.com.superauto.crm.relacionamento.entidade.FilaDeContatos;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;
import br.com.superauto.crm.relacionamento.servico.ConsultaTrabalhoService;
import br.com.superauto.crm.relacionamento.servico.LinhaDeTrabalho;
import br.com.superauto.crm.relacionamento.servico.PreviaDoContato;
import br.com.superauto.crm.relacionamento.servico.OrigemDoRegistro;
import br.com.superauto.crm.relacionamento.servico.RegistroDeAtendimentoService;
import br.com.superauto.crm.relacionamento.servico.ResumoDoPainel;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

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

import java.util.List;

/** FIL-01 a FIL-04 · Painel, filas e registro direto pelas filas (ATD-01, ATD-03). */
@RestController
@RequestMapping("/api/interno")
@RequiredArgsConstructor
public class TrabalhoRelacionamentoController {
    private final ConsultaTrabalhoService consulta;
    private final RegistroDeAtendimentoService registros;

    @GetMapping("/painel")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public ResumoDoPainel painel(JwtAuthenticationToken auth) {
        return consulta.painel(UsuarioAtual.vendedorRestrito(auth));
    }

    public record FiltrosFilaRequest(String busca, Integer cidade, Short etapa, Short tipoVeiculo) {
        FiltroDaFila filtro() {
            return new FiltroDaFila(busca, cidade, etapa, tipoVeiculo);
        }
    }

    /** FIL-02 / FIL-03 · Uma página da fila (parâmetros {@code pagina} e {@code tamanho}). */
    @GetMapping("/filas/{fila}")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public PagedModel<LinhaDeTrabalho> fila(
            @PathVariable String fila,
            @ModelAttribute FiltrosFilaRequest req,
            Pageable pagina,
            JwtAuthenticationToken auth) {
        var solicitada = new FilaSolicitada(
                FilaDeContatos.pelaRota(fila).atrasados(), req.filtro(), UsuarioAtual.vendedorRestrito(auth));
        return new PagedModel<>(consulta.contatos(solicitada, pagina));
    }

    /** FIL-04 · Uma página de aniversariantes de hoje, 7 ou 30 dias. */
    @GetMapping("/aniversariantes")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public PagedModel<LinhaDeTrabalho> aniversarios(
            @RequestParam(defaultValue = "0") int dias, Pageable pagina, JwtAuthenticationToken auth) {
        return new PagedModel<>(consulta.aniversarios(dias, UsuarioAtual.vendedorRestrito(auth), pagina));
    }

    @PostMapping("/filas/contratos/{id}/previa")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public PreviaDoContato prever(
            @PathVariable Long id,
            @RequestBody @Valid RegistroDeContatoRequest req,
            JwtAuthenticationToken auth) {
        return registros.prever(id, req.comando(OrigemDoRegistro.FILA), AutorDaRequisicao.de(auth));
    }

    @PostMapping("/filas/contratos/{id}/registro")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public AtendimentoGravado registrar(
            @PathVariable Long id,
            @RequestBody @Valid RegistroDeContatoRequest req,
            JwtAuthenticationToken auth) {
        return new AtendimentoGravado(registros.registrar(id, req.comando(OrigemDoRegistro.FILA), AutorDaRequisicao.de(auth)));
    }

    @PostMapping("/aniversariantes/contratos/{id}/registro")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public void aniversario(
            @PathVariable Long id, JwtAuthenticationToken auth) {
        registros.aniversario(id, AutorDaRequisicao.de(auth));
    }
}
