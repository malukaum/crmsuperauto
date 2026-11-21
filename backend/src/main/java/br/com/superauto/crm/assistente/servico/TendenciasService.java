package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.Assunto;
import br.com.superauto.crm.assistente.entidade.SinalDoAtendimento;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;
import br.com.superauto.crm.assistente.entidade.Tendencias;
import br.com.superauto.crm.assistente.repositorio.AnotacaoDoAtendimentoRepository;
import br.com.superauto.crm.assistente.repositorio.CarteiraParaAssistente;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.PedidoDaIa;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * IA-03 · Tendências da carteira: os indicadores são calculados a partir dos resumos (nunca pela IA); a leitura em
 * texto é da IA e recebe só os números agregados, sem dados de cliente. O vendedor vê só a carteira dele (RG-03).
 */
@Service
@RequiredArgsConstructor
public class TendenciasService {
    static final String SEM_DADOS = "Ainda não há atendimentos resumidos no período para ler tendências.";

    private final AnotacaoDoAtendimentoRepository anotacoes;
    private final CarteiraParaAssistente carteira;
    private final CalculoDeTendencias calculo;
    private final ExecutorDeIa executor;
    private final InterpretacaoDasRespostas interpretacao;
    private final Clock relogio;
    private final JsonMapper json = JsonMapper.builder().build();

    @Transactional(readOnly = true)
    public Tendencias indicadores(PeriodoDasTendencias periodo, Optional<UUID> vendedor) {
        return calculo.calcular(sinais(periodo, vendedor));
    }

    /** Pede a leitura à IA; grava só o registro de uso da chamada. */
    public LeituraDasTendencias leitura(PeriodoDasTendencias periodo, Optional<UUID> vendedor) {
        Tendencias tendencias = indicadores(periodo, vendedor);
        if (tendencias.atendimentosResumidos() == 0) return new LeituraDasTendencias(SEM_DADOS, null, null);
        var pedido = new PedidoDaIa(InstrucoesDoAssistente.LEITURA_DAS_TENDENCIAS,
                json.writeValueAsString(numeros(periodo, tendencias)), InstrucoesDoAssistente.TOKENS_DA_LEITURA);
        var execucao = executor.executar(TarefaDeIa.TENDENCIAS, pedido, Optional.empty(), interpretacao::textoLivre);
        return new LeituraDasTendencias(execucao.resultado(), execucao.provedor().nome(), execucao.modelo());
    }

    List<SinalDoAtendimento> sinais(PeriodoDasTendencias periodo, Optional<UUID> vendedor) {
        var inicio = periodo.inicioNoFuso(relogio.getZone());
        var fim = periodo.fimNoFuso(relogio.getZone());
        if (vendedor.isEmpty()) return anotacoes.sinais(inicio, fim);
        var associados = carteira.associadosDoVendedor(vendedor.get());
        return associados.isEmpty() ? List.of() : anotacoes.sinaisDosAssociados(inicio, fim, associados);
    }

    /** Só números agregados, em português, sem nenhum dado de cliente (IA-06). */
    static Map<String, Object> numeros(PeriodoDasTendencias periodo, Tendencias tendencias) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("periodo", Map.of("inicio", periodo.inicio().toString(), "fim", periodo.fim().toString()));
        dados.put("atendimentosResumidos", tendencias.atendimentosResumidos());
        Map<String, Long> assuntos = new LinkedHashMap<>();
        tendencias.porAssunto().entrySet().stream()
                .sorted(Map.Entry.<Assunto, Long>comparingByValue().reversed())
                .forEach(e -> assuntos.put(e.getKey().descricao(), e.getValue()));
        dados.put("atendimentosPorAssunto", assuntos);
        Map<String, Long> satisfacao = new LinkedHashMap<>();
        tendencias.porSatisfacao().forEach((s, n) -> satisfacao.put(s.name().toLowerCase(), n));
        dados.put("atendimentosPorSatisfacao", satisfacao);
        dados.put("atendimentosComRiscoDeCancelamento", tendencias.comRiscoDeCancelamento());
        dados.put("atendimentosComChanceAltaDeIndicar", tendencias.comChanceAltaDeIndicar());
        return dados;
    }
}
