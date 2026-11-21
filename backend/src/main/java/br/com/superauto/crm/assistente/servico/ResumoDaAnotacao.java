package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.AnexoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.AnotacaoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.Assunto;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.ResumoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.SituacaoDoResumo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IA-01 / IA-02 · A anotação de um atendimento como a ficha mostra: o que foi anotado, a situação do processamento e o
 * resumo gerado pela IA (marcado com o provedor e o modelo que o gerou).
 */
public record ResumoDaAnotacao(
        Long atendimentoId,
        SituacaoDoResumo situacao,
        String texto,
        List<AnexoAnotado> anexos,
        String resumoCurto,
        List<String> assuntos,
        String satisfacao,
        List<String> objecoes,
        List<String> interesses,
        boolean riscoDeCancelamento,
        String chanceDeIndicar,
        String proximaAcao,
        String provedor,
        String modelo,
        OffsetDateTime resumidoEm,
        String erro) {

    /** @param disponivel o conteúdo ainda está guardado (ainda não foi resumido) */
    public record AnexoAnotado(String nome, String tipoConteudo, long tamanho, boolean disponivel) {}

    static ResumoDaAnotacao de(AnotacaoDoAtendimento anotacao) {
        var resumo = anotacao.resumo();
        return new ResumoDaAnotacao(
                anotacao.getAtendimentoId(),
                anotacao.getSituacao(),
                anotacao.getTexto().orElse(null),
                anotacao.getAnexos().stream().map(ResumoDaAnotacao::anexo).toList(),
                resumo.map(ResumoDoAtendimento::resumoCurto).orElse(null),
                resumo.map(r -> r.assuntos().stream().sorted().map(Assunto::descricao).toList()).orElse(List.of()),
                resumo.map(r -> r.satisfacao().name()).orElse(null),
                resumo.map(ResumoDoAtendimento::objecoes).orElse(List.of()),
                resumo.map(ResumoDoAtendimento::interesses).orElse(List.of()),
                resumo.map(ResumoDoAtendimento::riscoDeCancelamento).orElse(false),
                resumo.map(r -> r.chanceDeIndicar().name()).orElse(null),
                resumo.map(ResumoDoAtendimento::proximaAcao).orElse(null),
                anotacao.getProvedor().map(ProvedorDeIa::nome).orElse(null),
                anotacao.getModelo().orElse(null),
                anotacao.getResumidoEm().orElse(null),
                anotacao.getErro().orElse(null));
    }

    private static AnexoAnotado anexo(AnexoDoAtendimento anexo) {
        return new AnexoAnotado(anexo.getNome(), anexo.getTipoConteudo(), anexo.getTamanho(),
                anexo.getConteudo().isPresent());
    }
}
