package br.com.superauto.crm.cadastro.api;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.superauto.crm.cadastro.entidade.Classificacao;
import br.com.superauto.crm.cadastro.entidade.DadosContato;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.MeioContato;
import br.com.superauto.crm.cadastro.entidade.Perfil;
import br.com.superauto.crm.cadastro.entidade.PotencialIndicacao;
import br.com.superauto.crm.cadastro.entidade.TipoContatoAssociado;
import br.com.superauto.crm.cadastro.servico.AlteracaoContato;
import br.com.superauto.crm.cadastro.servico.Autor;
import br.com.superauto.crm.cadastro.servico.ConsultaAssociadoService;
import br.com.superauto.crm.cadastro.servico.FichaAberta;
import br.com.superauto.crm.cadastro.servico.FichaAssociado;
import br.com.superauto.crm.cadastro.servico.GestaoAssociadoService;
import br.com.superauto.crm.cadastro.servico.PessoaCadastrada;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Associado: busca pelo documento no cadastro (CAR-02), ficha (CAR-03), contatos (CAR-04), classificação (CAR-06),
 * dados pessoais e documento completo (RG-04). O vendedor só enxerga e altera quem tem contrato dele (RG-03).
 */
@RestController
@RequestMapping("/api/interno/associados")
public class AssociadosController {

    private final ConsultaAssociadoService consulta;
    private final GestaoAssociadoService gestao;

    public AssociadosController(ConsultaAssociadoService consulta, GestaoAssociadoService gestao) {
        this.consulta = consulta;
        this.gestao = gestao;
    }

    public record DocumentoCompleto(String documento) {
    }

    public record DadosPessoaisRequest(
            int versao,
            @NotBlank(message = "Informe o nome.") @Size(max = 200, message = "O nome pode ter até 200 caracteres.")
            String nome,
            LocalDate dataNascimento,
            @NotNull(message = "Informe a cidade e a UF.") Integer municipioId) {
    }

    public record ClassificacaoRequest(
            int versao,
            @NotNull(message = "Escolha o perfil.") Perfil perfil,
            @NotNull(message = "Escolha o potencial de indicação.") PotencialIndicacao potencial) {
    }

    public record ContatoRequest(
            int versao,
            @NotNull(message = "Escolha o tipo do contato.") TipoContatoAssociado tipo,
            @NotBlank(message = "Informe o contato.") @Size(max = 254, message = "O contato pode ter até 254 caracteres.")
            String valor,
            boolean principal,
            boolean aceitaContato) {

        DadosContato dados() {
            return new DadosContato(MeioContato.de(tipo, valor), principal, aceitaContato);
        }
    }

    /** CAR-02 · O cadastro começa pelo CPF/CNPJ: se a pessoa já existe, os dados dela são carregados. */
    @GetMapping
    @PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
    public PessoaCadastrada porDocumento(@RequestParam String documento) {
        return consulta.porDocumento(Documento.de(documento))
                .orElseThrow(() -> NegocioException.naoEncontrado("Nenhuma pessoa com este documento na carteira."));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    public FichaAssociado ficha(@PathVariable Long id, JwtAuthenticationToken auth) {
        return consulta.ficha(id, AutorDoToken.escopo(auth));
    }

    /** RG-04 · O atendente vê o documento mascarado; gerencial e vendedor veem completo, com registro na auditoria. */
    @GetMapping("/{id}/documento")
    @PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
    public DocumentoCompleto documento(@PathVariable Long id, JwtAuthenticationToken auth) {
        return new DocumentoCompleto(gestao.exibirDocumento(id, autor(auth)));
    }

    /** O atendente só altera contatos (CAR-04): dados pessoais ficam com gerencial e vendedor. */
    @PutMapping("/{id}/dados-pessoais")
    @PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void corrigirDados(@PathVariable Long id, @Valid @RequestBody DadosPessoaisRequest req,
                              JwtAuthenticationToken auth) {
        gestao.corrigirDados(new FichaAberta(id, req.versao()),
                new DadosPessoais(req.nome(), req.dataNascimento(), req.municipioId()), autor(auth));
    }

    @PutMapping("/{id}/classificacao")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void classificar(@PathVariable Long id, @Valid @RequestBody ClassificacaoRequest req,
                            JwtAuthenticationToken auth) {
        gestao.classificar(new FichaAberta(id, req.versao()), new Classificacao(req.perfil(), req.potencial()),
                autor(auth));
    }

    @PostMapping("/{id}/contatos")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adicionarContato(@PathVariable Long id, @Valid @RequestBody ContatoRequest req,
                                 JwtAuthenticationToken auth) {
        gestao.adicionarContato(new FichaAberta(id, req.versao()), req.dados(), autor(auth));
    }

    @PutMapping("/{id}/contatos/{contatoId}")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarContato(@PathVariable Long id, @PathVariable Long contatoId,
                               @Valid @RequestBody ContatoRequest req, JwtAuthenticationToken auth) {
        gestao.alterarContato(new FichaAberta(id, req.versao()), new AlteracaoContato(contatoId, req.dados()),
                autor(auth));
    }

    @DeleteMapping("/{id}/contatos/{contatoId}")
    @PreAuthorize(Papeis.EQUIPE_INTERNA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerContato(@PathVariable Long id, @PathVariable Long contatoId, @RequestParam int versao,
                               JwtAuthenticationToken auth) {
        gestao.removerContato(new FichaAberta(id, versao), contatoId, autor(auth));
    }

    private static Autor autor(JwtAuthenticationToken auth) {
        return AutorDoToken.autor(auth);
    }
}
