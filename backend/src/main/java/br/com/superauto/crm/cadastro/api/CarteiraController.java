package br.com.superauto.crm.cadastro.api;

import br.com.superauto.crm.cadastro.entidade.Perfil;
import br.com.superauto.crm.cadastro.entidade.PotencialIndicacao;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.repositorio.FiltroCarteira;
import br.com.superauto.crm.cadastro.repositorio.OrdemCarteira;
import br.com.superauto.crm.cadastro.repositorio.TermoBusca;
import br.com.superauto.crm.cadastro.servico.ConsultaCarteiraService;
import br.com.superauto.crm.cadastro.servico.ExportacaoCarteiraService;
import br.com.superauto.crm.cadastro.servico.Exportador;
import br.com.superauto.crm.cadastro.servico.Ordenacao;
import br.com.superauto.crm.cadastro.servico.PaginaCarteira;
import br.com.superauto.crm.cadastro.servico.PaginaSolicitada;
import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CAR-01 · Buscar e filtrar a carteira; CAR-07 · Exportar a lista filtrada. O vendedor só vê e exporta os próprios
 * contratos: o filtro de vendedor que ele mandar é ignorado (RG-03).
 */
@RestController
@RequestMapping("/api/interno/carteira")
public class CarteiraController {

    private final ConsultaCarteiraService consulta;
    private final ExportacaoCarteiraService exportacao;
    private final Clock relogio;

    public CarteiraController(ConsultaCarteiraService consulta, ExportacaoCarteiraService exportacao, Clock relogio) {
        this.consulta = consulta;
        this.exportacao = exportacao;
        this.relogio = relogio;
    }

    /** Filtros, ordenação e página como chegam na URL; tudo opcional. */
    public record FiltroCarteiraRequest(String busca, Fila fila, Integer cidade, SituacaoContrato situacao,
                                        Short etapa, Short tipoVeiculo, Perfil perfil, PotencialIndicacao potencial,
                                        UUID vendedor, OrdemCarteira ordem, Boolean decrescente, Integer pagina,
                                        Integer tamanho, Boolean excluirCancelados) {

        private static final int PRIMEIRA_PAGINA = 0;
        private static final int TAMANHO_PADRAO = 25;

        FiltroCarteira filtro() {
            return new FiltroCarteira(TermoBusca.de(busca), Optional.ofNullable(fila), Optional.ofNullable(cidade),
                    Optional.ofNullable(situacao), Optional.ofNullable(etapa), Optional.ofNullable(tipoVeiculo),
                    Optional.ofNullable(perfil), Optional.ofNullable(potencial), Optional.ofNullable(vendedor),
                    Boolean.TRUE.equals(excluirCancelados));
        }

        Ordenacao ordenacao() {
            return ordem == null ? Ordenacao.PADRAO : new Ordenacao(ordem, Boolean.TRUE.equals(decrescente));
        }

        PaginaSolicitada paginaSolicitada() {
            return new PaginaSolicitada(Optional.ofNullable(pagina).orElse(PRIMEIRA_PAGINA),
                    Optional.ofNullable(tamanho).orElse(TAMANHO_PADRAO), ordenacao());
        }
    }

    @GetMapping
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public PaginaCarteira buscar(@ModelAttribute FiltroCarteiraRequest req, JwtAuthenticationToken auth) {
        return consulta.buscar(req.filtro(), req.paginaSolicitada(), AutorDoToken.escopo(auth));
    }

    /** Excel (.xlsx, padrão) ou CSV, com a lista exatamente como filtrada. O atendente não exporta (proposta). */
    @GetMapping("/exportacao")
    @PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
    public ResponseEntity<byte[]> exportar(@ModelAttribute FiltroCarteiraRequest req,
                                           @RequestParam(defaultValue = "XLSX") FormatoExportacao formato,
                                           JwtAuthenticationToken auth) {
        var exportador = new Exportador(UsuarioAtual.exigirId(auth), AutorDoToken.escopo(auth),
                UsuarioAtual.temPapel(auth, Papeis.GERENCIAL));
        byte[] arquivo = formato.gerar(exportacao.exportar(req.filtro(), req.ordenacao(), exportador));
        String nome = "carteira-" + LocalDate.now(relogio) + "." + formato.extensao();
        return ResponseEntity.ok()
                .contentType(formato.tipo())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .body(arquivo);
    }
}
