package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.AnexoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.AnotacaoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.ResumoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.SituacaoDoResumo;
import br.com.superauto.crm.assistente.repositorio.AnotacaoDoAtendimentoRepository;
import br.com.superauto.crm.assistente.repositorio.CarteiraParaAssistente;
import br.com.superauto.crm.assistente.repositorio.ExtratorDeTexto;
import br.com.superauto.crm.erro.NegocioException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * IA-02 · As etapas com banco do resumo de um atendimento, cada uma na sua transação curta: preparar o material
 * (pseudonimizado), guardar o resumo, registrar a falha ou adiar. A chamada ao provedor fica entre elas, fora de
 * transação.
 */
@Service
@RequiredArgsConstructor
public class EtapasDoResumo {
    static final int LOTE = 10;
    static final int LIMITE_DE_CARACTERES = 60_000;
    static final Duration ESPERA_SEM_PROVEDOR = Duration.ofMinutes(5);

    private final AnotacaoDoAtendimentoRepository anotacoes;
    private final CarteiraParaAssistente carteira;
    private final ExtratorDeTexto extrator;
    private final Pseudonimizacao pseudonimizacao;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public List<Long> pendentes() {
        return anotacoes.pendentes(OffsetDateTime.now(relogio), Limit.of(LOTE));
    }

    /** Monta o texto das anotações e dos anexos e tira dele o que identifica pessoas (IA-06). */
    @Transactional(readOnly = true)
    public Optional<MaterialDoResumo> preparar(Long atendimentoId) {
        return anotacoes.findById(atendimentoId)
                .filter(a -> a.getSituacao() == SituacaoDoResumo.AGUARDANDO)
                .map(anotacao -> new MaterialDoResumo(atendimentoId, pseudonimizacao.pseudonimizar(
                        texto(anotacao), carteira.identificaveis(anotacao.getAssociadoId()))));
    }

    @Transactional
    public void concluir(Long atendimentoId, Execucao<ResumoDoAtendimento> execucao) {
        exigir(atendimentoId).registrarResumo(execucao.resultado(), execucao.provedor(), execucao.modelo(),
                OffsetDateTime.now(relogio));
    }

    @Transactional
    public void falhar(Long atendimentoId, String motivo) {
        exigir(atendimentoId).registrarFalha(motivo, OffsetDateTime.now(relogio));
    }

    /** Sem provedor disponível: espera sem gastar tentativa (IA-01). */
    @Transactional
    public void adiar(Long atendimentoId) {
        exigir(atendimentoId).adiarSemTentativa(OffsetDateTime.now(relogio).plus(ESPERA_SEM_PROVEDOR));
    }

    private AnotacaoDoAtendimento exigir(Long atendimentoId) {
        return anotacoes.buscarParaAlteracao(atendimentoId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Anotação não encontrada."));
    }

    private String texto(AnotacaoDoAtendimento anotacao) {
        var texto = new StringBuilder();
        anotacao.getTexto().ifPresent(t -> texto.append("Anotações do atendimento:\n").append(t).append("\n"));
        for (AnexoDoAtendimento anexo : anotacao.getAnexos()) texto.append(textoDoAnexo(anexo));
        return texto.length() > LIMITE_DE_CARACTERES ? texto.substring(0, LIMITE_DE_CARACTERES) : texto.toString();
    }

    /** Imagens não vão ao provedor no protótipo: sem OCR no servidor, não há como pseudonimizá-las (IA-06). */
    private String textoDoAnexo(AnexoDoAtendimento anexo) {
        return anexo.getConteudo()
                .flatMap(conteudo -> extrator.extrair(anexo.getTipoConteudo(), conteudo))
                .map(texto -> "\nAnexo \"" + anexo.getNome() + "\":\n" + texto + "\n")
                .orElse("\nAnexo \"" + anexo.getNome() + "\" (" + anexo.getTipoConteudo()
                        + ") recebido, sem texto para ler.\n");
    }
}
