package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.relacionamento.entidade.FilaDeContatos;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;
import br.com.superauto.crm.relacionamento.servico.AutorDoContato;
import br.com.superauto.crm.relacionamento.servico.ConsultaTrabalhoService;
import br.com.superauto.crm.relacionamento.servico.ContatoDoMes;
import br.com.superauto.crm.relacionamento.servico.LinhaDeTrabalho;
import br.com.superauto.crm.relacionamento.servico.PreviaDoContato;
import br.com.superauto.crm.relacionamento.servico.RegistroDeContato;
import br.com.superauto.crm.relacionamento.servico.RegistroNaFilaService;
import br.com.superauto.crm.relacionamento.servico.ResumoDoPainel;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/interno")
@RequiredArgsConstructor
public class TrabalhoRelacionamentoController {
    private final ConsultaTrabalhoService consulta;
    private final RegistroNaFilaService registros;

    @GetMapping("/atendimentos")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public List<ContatoDoMes> atendimentos(JwtAuthenticationToken auth) {
        return consulta.atendimentosMes(UsuarioAtual.vendedorRestrito(auth));
    }

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

    public record RegistroRequest(
            @NotNull(message = "Informe a versão do acompanhamento.")
                    @Min(value = 0, message = "A versão não pode ser negativa.")
                    Integer versao,
            @NotNull(message = "Informe o resultado do contato.") ResultadoContato resultado,
            LocalDate retorno,
            @Size(max = 2000, message = "O resumo deve ter até 2.000 caracteres.")
                    String descricao) {
        RegistroDeContato comando() {
            return new RegistroDeContato(versao, resultado, retorno, descricao);
        }
    }

    @PostMapping("/filas/contratos/{id}/previa")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public PreviaDoContato prever(
            @PathVariable Long id,
            @RequestBody @Valid RegistroRequest req,
            JwtAuthenticationToken auth) {
        return registros.prever(id, req.comando(), autor(auth));
    }

    @PostMapping("/filas/contratos/{id}/registro")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public void registrar(
            @PathVariable Long id,
            @RequestBody @Valid RegistroRequest req,
            JwtAuthenticationToken auth) {
        registros.registrar(id, req.comando(), autor(auth));
    }

    public record AniversarioRequest(boolean confirmarRepeticao) {}

    @PostMapping("/aniversariantes/contratos/{id}/registro")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public void aniversario(
            @PathVariable Long id,
            @RequestBody AniversarioRequest req,
            JwtAuthenticationToken auth) {
        registros.aniversario(id, req.confirmarRepeticao(), autor(auth));
    }

    private static AutorDoContato autor(JwtAuthenticationToken auth) {
        return new AutorDoContato(UsuarioAtual.exigirId(auth), UsuarioAtual.vendedorRestrito(auth));
    }
}
