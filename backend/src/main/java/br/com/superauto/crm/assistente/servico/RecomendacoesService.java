package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.AnotacaoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.Recomendacao;
import br.com.superauto.crm.assistente.entidade.ResumoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;
import br.com.superauto.crm.assistente.entidade.Tendencias;
import br.com.superauto.crm.assistente.repositorio.AnotacaoDoAtendimentoRepository;
import br.com.superauto.crm.assistente.repositorio.CarteiraParaAssistente;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.PedidoDaIa;
import br.com.superauto.crm.erro.NegocioException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * IA-04 · Até 3 sugestões de abordagem para o próximo contato com o cliente, a partir dos resumos dele e das
 * tendências da carteira de quem pede. Só sugestão: nada é enviado nem alterado. Os resumos vão pseudonimizados.
 */
@Service
@RequiredArgsConstructor
public class RecomendacoesService {
    static final int RESUMOS_CONSIDERADOS = 10;

    private final AnotacaoDoAtendimentoRepository anotacoes;
    private final CarteiraParaAssistente carteira;
    private final TendenciasService tendencias;
    private final Pseudonimizacao pseudonimizacao;
    private final ExecutorDeIa executor;
    private final InterpretacaoDasRespostas interpretacao;
    private final Clock relogio;
    private final JsonMapper json = JsonMapper.builder().build();

    /** O resultado e quem o gerou. */
    public record Sugestoes(List<Recomendacao> recomendacoes, String provedor, String modelo) {}

    public Sugestoes recomendar(Long associadoId, QuemPede quem) {
        if (!carteira.associadoVisivel(associadoId, quem.vendedor()))
            throw NegocioException.naoEncontrado("Cliente não encontrado na carteira.");
        List<ResumoDoAtendimento> resumos = anotacoes.findByAssociadoIdOrderByRegistradoEmDesc(associadoId).stream()
                .map(AnotacaoDoAtendimento::resumo)
                .flatMap(Optional::stream)
                .limit(RESUMOS_CONSIDERADOS)
                .toList();
        if (resumos.isEmpty())
            throw NegocioException.conflito("sem-resumos",
                    "Ainda não há atendimentos resumidos deste cliente. Anote um atendimento para receber sugestões.");
        var periodo = PeriodoDasTendencias.de(Optional.empty(), Optional.empty(), LocalDate.now(relogio));
        Tendencias gerais = tendencias.indicadores(periodo, quem.vendedor());
        var texto = pseudonimizacao.pseudonimizar(material(resumos, periodo, gerais),
                carteira.identificaveis(associadoId));
        var pedido = new PedidoDaIa(InstrucoesDoAssistente.RECOMENDACOES, texto.texto(),
                InstrucoesDoAssistente.TOKENS_DAS_RECOMENDACOES);
        var execucao = executor.executar(TarefaDeIa.RECOMENDACOES, pedido, Optional.empty(),
                resposta -> interpretacao.recomendacoes(resposta, texto.marcadores()));
        return new Sugestoes(execucao.resultado(), execucao.provedor().nome(), execucao.modelo());
    }

    private String material(List<ResumoDoAtendimento> resumos, PeriodoDasTendencias periodo, Tendencias gerais) {
        var texto = new StringBuilder("Resumos recentes dos atendimentos do cliente:\n");
        for (var resumo : resumos) {
            texto.append("- ").append(resumo.resumoCurto())
                    .append(" | satisfação: ").append(resumo.satisfacao())
                    .append(" | risco de cancelamento: ").append(resumo.riscoDeCancelamento() ? "sim" : "não")
                    .append(" | objeções: ").append(String.join("; ", resumo.objecoes()))
                    .append(" | interesses: ").append(String.join("; ", resumo.interesses()))
                    .append('\n');
        }
        texto.append("\nIndicadores gerais da carteira (últimos 30 dias):\n")
                .append(json.writeValueAsString(TendenciasService.numeros(periodo, gerais)));
        return texto.toString();
    }
}
