package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.ConfiguracaoDoProvedor;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;
import br.com.superauto.crm.assistente.entidade.UsoDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.AcessoAoProvedor;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.PedidoDaIa;
import br.com.superauto.crm.assistente.repositorio.CofreDeChaves;
import br.com.superauto.crm.assistente.repositorio.ConfiguracaoDoProvedorRepository;
import br.com.superauto.crm.assistente.repositorio.RotaDaTarefaRepository;
import br.com.superauto.crm.assistente.repositorio.UsoDeIaRepository;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;
import br.com.superauto.crm.erro.NegocioException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * IA-05 · Executa uma tarefa do assistente no provedor certo: o principal da tarefa e, se ele falhar, estiver
 * desligado ou sem limite no mês, o reserva. Cada tentativa fica registrada (provedor, tokens, custo, se foi o
 * reserva), sem o conteúdo (IA-06).
 *
 * <p>Não abre transação: a chamada ao provedor pode levar segundos e não deve segurar conexão nem trava do banco.
 */
@Slf4j
@Service
public class ExecutorDeIa {
    static final String CODIGO_SEM_PROVEDOR = "ia-indisponivel";

    private final RotaDaTarefaRepository rotas;
    private final ConfiguracaoDoProvedorRepository configuracoes;
    private final UsoDeIaRepository usos;
    private final CofreDeChaves cofre;
    private final SelecaoDeProvedores selecao;
    private final Map<ProvedorDeIa, ClienteDeIa> clientes;
    private final Clock relogio;

    public ExecutorDeIa(RotaDaTarefaRepository rotas, ConfiguracaoDoProvedorRepository configuracoes,
                        UsoDeIaRepository usos, CofreDeChaves cofre, SelecaoDeProvedores selecao,
                        List<ClienteDeIa> clientes, Clock relogio) {
        this.rotas = rotas;
        this.configuracoes = configuracoes;
        this.usos = usos;
        this.cofre = cofre;
        this.selecao = selecao;
        this.clientes = clientes.stream().collect(Collectors.toMap(ClienteDeIa::provedor, Function.identity(),
                (a, b) -> a, () -> new EnumMap<>(ProvedorDeIa.class)));
        this.relogio = relogio;
    }

    /**
     * @param interpretar lê a resposta no formato combinado; se recusar, a tentativa conta como falha e o próximo
     *                    provedor é chamado
     * @throws NegocioException {@code ia-indisponivel} quando nenhum provedor da tarefa está pronto para uso
     * @throws IntegracaoIndisponivelException quando todos os provedores disponíveis falharam
     */
    public <T> Execucao<T> executar(TarefaDeIa tarefa, PedidoDaIa pedido, Optional<Long> atendimentoId,
                                    Function<String, T> interpretar) {
        var candidatos = candidatos(tarefa);
        if (candidatos.isEmpty())
            throw NegocioException.conflito(CODIGO_SEM_PROVEDOR,
                    "O assistente de IA está desligado ou sem provedor disponível para esta tarefa.");
        List<String> falhas = new ArrayList<>();
        for (var candidato : candidatos) {
            var configuracao = candidato.configuracao();
            var chamada = new UsoDeIa.Chamada(configuracao.getProvedor(), configuracao.getModelo().orElseThrow(), tarefa,
                    atendimentoId.orElse(null), candidato.reserva(), OffsetDateTime.now(relogio));
            try {
                var resposta = cliente(configuracao.getProvedor()).gerar(pedido, acesso(configuracao));
                T resultado = interpretar.apply(resposta.texto());
                usos.save(UsoDeIa.sucesso(chamada, resposta.tokensEntrada(), resposta.tokensSaida(),
                        configuracao.custoDe(resposta.tokensEntrada(), resposta.tokensSaida()).orElse(null)));
                return new Execucao<>(resultado, configuracao.getProvedor(), chamada.modelo(), candidato.reserva());
            } catch (RuntimeException e) {
                log.warn("Tarefa {} falhou em {}: {}", tarefa, configuracao.getProvedor(), e.getMessage());
                usos.save(UsoDeIa.falha(chamada, e.getMessage()));
                falhas.add(configuracao.getProvedor().nome() + ": " + e.getMessage());
            }
        }
        throw new IntegracaoIndisponivelException(
                "Nenhum provedor de IA conseguiu atender agora (" + String.join("; ", falhas) + ").", null);
    }

    private List<SelecaoDeProvedores.Candidato> candidatos(TarefaDeIa tarefa) {
        var rota = rotas.findById(tarefa).orElseThrow(
                () -> new IllegalStateException("Tarefa de IA sem configuração: " + tarefa));
        Map<ProvedorDeIa, ConfiguracaoDoProvedor> porProvedor = new EnumMap<>(ProvedorDeIa.class);
        configuracoes.findAllById(rota.ordemDeTentativa()).forEach(c -> porProvedor.put(c.getProvedor(), c));
        Map<ProvedorDeIa, BigDecimal> gastos = new EnumMap<>(ProvedorDeIa.class);
        porProvedor.keySet().forEach(p -> gastos.put(p, gastoDoMes(p)));
        return selecao.candidatos(rota, porProvedor, gastos);
    }

    /** IA-05 · O gasto do mês corrente (Brasília, RG-01) no provedor. */
    public BigDecimal gastoDoMes(ProvedorDeIa provedor) {
        var inicio = LocalDate.now(relogio).withDayOfMonth(1).atStartOfDay(relogio.getZone());
        return usos.gastoNoPeriodo(provedor, inicio.toOffsetDateTime(), inicio.plusMonths(1).toOffsetDateTime());
    }

    ClienteDeIa cliente(ProvedorDeIa provedor) {
        var cliente = clientes.get(provedor);
        if (cliente == null) throw new IllegalStateException("Sem adaptador para o provedor " + provedor);
        return cliente;
    }

    AcessoAoProvedor acesso(ConfiguracaoDoProvedor configuracao) {
        return new AcessoAoProvedor(configuracao.getModelo().orElseThrow(),
                cofre.decifrar(configuracao.getChaveCifrada().orElseThrow()));
    }
}
