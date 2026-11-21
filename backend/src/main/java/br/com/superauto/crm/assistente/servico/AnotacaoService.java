package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.AnotacaoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.NovaAnotacao;
import br.com.superauto.crm.assistente.repositorio.AnotacaoDoAtendimentoRepository;
import br.com.superauto.crm.assistente.repositorio.AtendimentosParaAssistente;
import br.com.superauto.crm.assistente.repositorio.CarteiraParaAssistente;
import br.com.superauto.crm.erro.NegocioException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * IA-01 · Anotar um atendimento com texto livre e anexos. Grava na hora; o resumo é feito em segundo plano por
 * {@link ProcessamentoDeAnotacoes}. O vendedor só anota e vê atendimentos da carteira dele (RG-03).
 */
@Service
@RequiredArgsConstructor
public class AnotacaoService {
    private final AnotacaoDoAtendimentoRepository anotacoes;
    private final AtendimentosParaAssistente atendimentos;
    private final CarteiraParaAssistente carteira;

    @Transactional
    public ResumoDaAnotacao anotar(PedidoDeAnotacao pedido, QuemPede quem) {
        Long associadoId = atendimentos.associadoDoAtendimento(pedido.atendimentoId(), quem.vendedor())
                .orElseThrow(() -> NegocioException.naoEncontrado("Atendimento não encontrado."));
        if (anotacoes.existsById(pedido.atendimentoId()))
            throw NegocioException.conflito("anotacao-existente", "Este atendimento já tem anotações.");
        var anotacao = AnotacaoDoAtendimento.registrar(
                new NovaAnotacao(pedido.atendimentoId(), associadoId, pedido.texto(), pedido.anexos()), quem.id());
        return ResumoDaAnotacao.de(anotacoes.save(anotacao));
    }

    @Transactional(readOnly = true)
    public Optional<ResumoDaAnotacao> doAtendimento(Long atendimentoId, QuemPede quem) {
        return atendimentos.associadoDoAtendimento(atendimentoId, quem.vendedor())
                .flatMap(associado -> anotacoes.findById(atendimentoId))
                .map(ResumoDaAnotacao::de);
    }

    /** As anotações e os resumos de todos os atendimentos do associado, dos mais recentes para os mais antigos. */
    @Transactional(readOnly = true)
    public List<ResumoDaAnotacao> doAssociado(Long associadoId, QuemPede quem) {
        if (!carteira.associadoVisivel(associadoId, quem.vendedor()))
            throw NegocioException.naoEncontrado("Cliente não encontrado na carteira.");
        return anotacoes.findByAssociadoIdOrderByRegistradoEmDesc(associadoId).stream()
                .map(ResumoDaAnotacao::de)
                .toList();
    }
}
