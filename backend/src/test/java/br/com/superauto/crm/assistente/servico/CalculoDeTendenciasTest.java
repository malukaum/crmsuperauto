package br.com.superauto.crm.assistente.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.assistente.entidade.Assunto;
import br.com.superauto.crm.assistente.entidade.ChanceDeIndicar;
import br.com.superauto.crm.assistente.entidade.Satisfacao;
import br.com.superauto.crm.assistente.entidade.SinalDoAtendimento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class CalculoDeTendenciasTest {

    @Test
    @DisplayName("IA-03: dados 10 atendimentos no período, 4 com o assunto guincho, guincho aparece em 4 deles")
    void criterioDeAceite() {
        List<SinalDoAtendimento> sinais = new ArrayList<>();
        for (int i = 0; i < 4; i++) sinais.add(new SinalDoAtendimento(List.of("GUINCHO"), Satisfacao.NEGATIVA, true, ChanceDeIndicar.BAIXA));
        for (int i = 0; i < 6; i++) sinais.add(new SinalDoAtendimento(List.of("MENSALIDADE"), Satisfacao.POSITIVA, false, ChanceDeIndicar.ALTA));

        var tendencias = new CalculoDeTendencias().calcular(sinais);

        assertThat(tendencias.atendimentosResumidos()).isEqualTo(10);
        assertThat(tendencias.porAssunto()).containsEntry(Assunto.GUINCHO, 4L).containsEntry(Assunto.MENSALIDADE, 6L);
        assertThat(tendencias.comRiscoDeCancelamento()).isEqualTo(4);
        assertThat(tendencias.comChanceAltaDeIndicar()).isEqualTo(6);
    }
}
