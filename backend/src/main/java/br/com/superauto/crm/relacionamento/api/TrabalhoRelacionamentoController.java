package br.com.superauto.crm.relacionamento.api;

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

    @GetMapping("/filas/{fila}")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public List<LinhaDeTrabalho> fila(
            @PathVariable String fila,
            @ModelAttribute FiltrosFilaRequest req,
            JwtAuthenticationToken auth) {
        return consulta.contatos(
                FilaDeContatos.pelaRota(fila).atrasados(),
                req.filtro(),
                UsuarioAtual.vendedorRestrito(auth));
    }

    @GetMapping("/aniversariantes")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public List<LinhaDeTrabalho> aniversarios(
            @RequestParam(defaultValue = "0") int dias, JwtAuthenticationToken auth) {
        return consulta.aniversarios(dias, UsuarioAtual.vendedorRestrito(auth));
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
    public void registrar(
            @PathVariable Long id,
            @RequestBody @Valid RegistroDeContatoRequest req,
            JwtAuthenticationToken auth) {
        registros.registrar(id, req.comando(OrigemDoRegistro.FILA), AutorDaRequisicao.de(auth));
    }

    public record AniversarioRequest(boolean confirmarRepeticao) {}

    @PostMapping("/aniversariantes/contratos/{id}/registro")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public void aniversario(
            @PathVariable Long id,
            @RequestBody AniversarioRequest req,
            JwtAuthenticationToken auth) {
        registros.aniversario(id, req.confirmarRepeticao(), AutorDaRequisicao.de(auth));
    }
}
