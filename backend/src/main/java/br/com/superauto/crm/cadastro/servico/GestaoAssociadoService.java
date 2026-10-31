package br.com.superauto.crm.cadastro.servico;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.Classificacao;
import br.com.superauto.crm.cadastro.entidade.DadosContato;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.DocumentoExibido;
import br.com.superauto.crm.cadastro.repositorio.AssociadoRepository;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;
import br.com.superauto.crm.cadastro.repositorio.ReferenciasDoCadastro;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.erro.NegocioException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comandos sobre o associado: dados pessoais e contatos (CAR-04), classificação (CAR-06), exibição do documento
 * completo (RG-04) e anonimização (CAR-08). As regras ficam no agregado {@link Associado}; cada mudança sai como
 * evento de domínio para a auditoria (RG-08). O vendedor só altera quem tem contrato dele (RG-03).
 */
@Service
public class GestaoAssociadoService {

    private final AssociadoRepository associados;
    private final ContratoRepository contratos;
    private final ReferenciasDoCadastro referencias;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public GestaoAssociadoService(AssociadoRepository associados, ContratoRepository contratos,
                                  ReferenciasDoCadastro referencias, ApplicationEventPublisher eventos, Clock relogio) {
        this.associados = associados;
        this.contratos = contratos;
        this.referencias = referencias;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Transactional
    public void corrigirDados(FichaAberta ficha, DadosPessoais dados, Autor autor) {
        if (referencias.municipio(dados.municipioId()).isEmpty()) {
            throw NegocioException.invalido("cidade-invalida", "Escolha uma cidade da lista.");
        }
        alterar(ficha, autor, associado -> associado.corrigirDados(dados, autor.id()));
    }

    @Transactional
    public void classificar(FichaAberta ficha, Classificacao classificacao, Autor autor) {
        alterar(ficha, autor, associado -> associado.classificar(classificacao, autor.id()));
    }

    @Transactional
    public void adicionarContato(FichaAberta ficha, DadosContato contato, Autor autor) {
        alterar(ficha, autor, associado -> associado.adicionarContato(contato, autor.id()));
    }

    @Transactional
    public void alterarContato(FichaAberta ficha, AlteracaoContato alteracao, Autor autor) {
        alterar(ficha, autor, associado -> associado.alterarContato(alteracao.contatoId(), alteracao.dados(), autor.id()));
    }

    @Transactional
    public void removerContato(FichaAberta ficha, Long contatoId, Autor autor) {
        alterar(ficha, autor, associado -> associado.removerContato(contatoId, autor.id()));
    }

    /** RG-04 · O documento completo, para gerencial e vendedor; cada exibição vai para a auditoria (proposta). */
    @Transactional
    public String exibirDocumento(Long associadoId, Autor autor) {
        Associado associado = carregarVisivel(associadoId, autor.escopo());
        String documento = associado.getDocumento().map(Documento::formatado)
                .orElseThrow(() -> NegocioException.naoEncontrado("Este cadastro foi anonimizado e não tem mais documento."));
        eventos.publishEvent(new DocumentoExibido(associadoId, autor.id()));
        return documento;
    }

    /** CAR-08 · Só o gerencial, e só com todos os contratos cancelados (a regra está no agregado). */
    @Transactional
    public void anonimizar(FichaAberta ficha, Autor autor) {
        alterar(ficha, autor, associado -> associado.anonimizar(contratos.findByAssociadoId(associado.getId()),
                OffsetDateTime.now(relogio), autor.id()));
    }

    /** Carrega, confere a versão vista, aplica a mudança e salva (o {@code save} publica os eventos). */
    private void alterar(FichaAberta ficha, Autor autor, Consumer<Associado> mudanca) {
        Associado associado = carregarVisivel(ficha.associadoId(), autor.escopo());
        associado.conferirVersao(ficha.versao());
        mudanca.accept(associado);
        associados.save(associado);
    }

    private Associado carregarVisivel(Long associadoId, EscopoCarteira escopo) {
        boolean visivel = escopo.vendedor().map(v -> contratos.existsByAssociadoIdAndVendedorId(associadoId, v)).orElse(true);
        return associados.findById(associadoId).filter(a -> visivel)
                .orElseThrow(() -> NegocioException.naoEncontrado("Cliente não encontrado na carteira."));
    }
}
