package br.com.superauto.crm.assistente.api;

import br.com.superauto.crm.assistente.entidade.NovoAnexo;
import br.com.superauto.crm.assistente.servico.AnotacaoService;
import br.com.superauto.crm.assistente.servico.PedidoDeAnotacao;
import br.com.superauto.crm.assistente.servico.ResumoDaAnotacao;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** IA-01 / IA-02 · Anotar um atendimento (texto e anexos) e ver as anotações e os resumos gerados pela IA. */
@RestController
@RequestMapping("/api/interno")
@RequiredArgsConstructor
public class AnotacoesController {
    private final AnotacaoService anotacoes;

    @PostMapping(path = "/atendimentos/{id}/anotacao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public ResumoDaAnotacao anotar(
            @PathVariable Long id,
            @RequestParam(required = false) String texto,
            @RequestParam(name = "anexos", required = false) List<MultipartFile> anexos,
            JwtAuthenticationToken auth) {
        var pedido = new PedidoDeAnotacao(id, texto, anexos == null ? List.of() : anexos.stream().map(AnotacoesController::anexo).toList());
        return anotacoes.anotar(pedido, QuemPedeDoToken.de(auth));
    }

    @GetMapping("/atendimentos/{id}/anotacao")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public ResponseEntity<ResumoDaAnotacao> doAtendimento(@PathVariable Long id, JwtAuthenticationToken auth) {
        return ResponseEntity.of(anotacoes.doAtendimento(id, QuemPedeDoToken.de(auth)));
    }

    @GetMapping("/associados/{id}/anotacoes")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public List<ResumoDaAnotacao> doAssociado(@PathVariable Long id, JwtAuthenticationToken auth) {
        return anotacoes.doAssociado(id, QuemPedeDoToken.de(auth));
    }

    private static NovoAnexo anexo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) throw NegocioException.invalido("anexo-invalido", "Um dos anexos está vazio.");
        try {
            return new NovoAnexo(arquivo.getOriginalFilename(), arquivo.getContentType(), arquivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o anexo enviado.", e);
        }
    }
}
