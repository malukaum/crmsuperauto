package br.com.superauto.crm.assistente.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.assistente.entidade.ConfiguracoesDeTeste;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

class SelecaoDeProvedoresTest {
    private final SelecaoDeProvedores selecao = new SelecaoDeProvedores();
    private final BigDecimal limite = new BigDecimal("20");

    @Test
    @DisplayName("IA-05: com o principal no limite do mês, só o reserva é candidato, marcado como reserva")
    void principalNoLimite() {
        var rota = ConfiguracoesDeTeste.rota(TarefaDeIa.RESUMO, ProvedorDeIa.CLAUDE, ProvedorDeIa.GEMINI);
        var claude = ConfiguracoesDeTeste.provedor(ProvedorDeIa.CLAUDE, true, "m", "x", limite);
        var gemini = ConfiguracoesDeTeste.provedor(ProvedorDeIa.GEMINI, true, "m", "x", limite);

        var candidatos = selecao.candidatos(rota, Map.of(ProvedorDeIa.CLAUDE, claude, ProvedorDeIa.GEMINI, gemini),
                Map.of(ProvedorDeIa.CLAUDE, limite));

        assertThat(candidatos).singleElement().satisfies(c -> {
            assertThat(c.configuracao().getProvedor()).isEqualTo(ProvedorDeIa.GEMINI);
            assertThat(c.reserva()).isTrue();
        });
    }

    @Test
    @DisplayName("IA-05: provedor desligado ou sem chave nunca é chamado")
    void desligado() {
        var rota = ConfiguracoesDeTeste.rota(TarefaDeIa.RESUMO, ProvedorDeIa.CHATGPT, null);
        var chatgpt = ConfiguracoesDeTeste.provedor(ProvedorDeIa.CHATGPT, true, "m", null, limite);

        assertThat(selecao.candidatos(rota, Map.of(ProvedorDeIa.CHATGPT, chatgpt), Map.of())).isEmpty();
    }
}
