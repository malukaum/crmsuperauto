package br.com.superauto.crm.assistente.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.assistente.entidade.ConfiguracoesDeTeste;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;
import br.com.superauto.crm.assistente.entidade.UsoDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa;
import br.com.superauto.crm.assistente.repositorio.CofreDeChaves;
import br.com.superauto.crm.assistente.repositorio.ConfiguracaoDoProvedorRepository;
import br.com.superauto.crm.assistente.repositorio.RotaDaTarefaRepository;
import br.com.superauto.crm.assistente.repositorio.UsoDeIaRepository;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

class ExecutorDeIaTest {
    private static final ClienteDeIa.PedidoDaIa PEDIDO = new ClienteDeIa.PedidoDaIa("instruções", "conteúdo", 100);

    private final RotaDaTarefaRepository rotas = mock(RotaDaTarefaRepository.class);
    private final ConfiguracaoDoProvedorRepository configuracoes = mock(ConfiguracaoDoProvedorRepository.class);
    private final UsoDeIaRepository usos = mock(UsoDeIaRepository.class);
    private final CofreDeChaves cofre = mock(CofreDeChaves.class);

    /** Dublê de provedor: responde o texto dado ou falha. */
    private record ProvedorFalso(ProvedorDeIa provedor, String resposta) implements ClienteDeIa {
        @Override
        public RespostaDaIa gerar(PedidoDaIa pedido, AcessoAoProvedor acesso) {
            if (resposta == null) throw new FalhaNoProvedor("fora do ar");
            return new RespostaDaIa(resposta, 100, 20);
        }
    }

    private ExecutorDeIa executor(ClienteDeIa... clientes) {
        when(rotas.findById(TarefaDeIa.RESUMO)).thenReturn(Optional.of(
                ConfiguracoesDeTeste.rota(TarefaDeIa.RESUMO, ProvedorDeIa.CLAUDE, ProvedorDeIa.GEMINI)));
        when(configuracoes.findAllById(any())).thenReturn(List.of(
                ConfiguracoesDeTeste.provedor(ProvedorDeIa.CLAUDE, true, "claude-opus-5-5", "c1", BigDecimal.TEN),
                ConfiguracoesDeTeste.provedor(ProvedorDeIa.GEMINI, true, "gemini", "c2", BigDecimal.TEN)));
        when(usos.gastoNoPeriodo(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(cofre.decifrar(any())).thenReturn("chave");
        return new ExecutorDeIa(rotas, configuracoes, usos, cofre, new SelecaoDeProvedores(), List.of(clientes),
                Clock.systemUTC());
    }

    @Test
    @DisplayName("IA-05: com o Claude fora do ar, o Gemini (reserva) faz a tarefa, e as duas tentativas ficam registradas")
    void reservaAssume() {
        var executor = executor(new ProvedorFalso(ProvedorDeIa.CLAUDE, null), new ProvedorFalso(ProvedorDeIa.GEMINI, "ok"));

        var execucao = executor.executar(TarefaDeIa.RESUMO, PEDIDO, Optional.of(10L), texto -> texto);

        assertThat(execucao.provedor()).isEqualTo(ProvedorDeIa.GEMINI);
        assertThat(execucao.reserva()).isTrue();
        verify(usos, times(2)).save(any(UsoDeIa.class));
    }

    @Test
    @DisplayName("IA-05: resposta fora do formato no principal também passa a tarefa para o reserva")
    void respostaInvalidaVaiParaOReserva() {
        var executor = executor(new ProvedorFalso(ProvedorDeIa.CLAUDE, "texto solto"),
                new ProvedorFalso(ProvedorDeIa.GEMINI, "{}"));

        var execucao = executor.executar(TarefaDeIa.RESUMO, PEDIDO, Optional.empty(), texto -> {
            if (!texto.startsWith("{")) throw NegocioException.invalido("resposta-invalida", "fora do formato");
            return texto;
        });

        assertThat(execucao.provedor()).isEqualTo(ProvedorDeIa.GEMINI);
    }

    @Test
    @DisplayName("IA-05: sem nenhum provedor pronto para uso, a tarefa responde que o assistente está indisponível")
    void semProvedor() {
        var executor = executor(new ProvedorFalso(ProvedorDeIa.CLAUDE, "ok"));
        when(configuracoes.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> executor.executar(TarefaDeIa.RESUMO, PEDIDO, Optional.empty(), texto -> texto))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo(ExecutorDeIa.CODIGO_SEM_PROVEDOR);
    }
}
