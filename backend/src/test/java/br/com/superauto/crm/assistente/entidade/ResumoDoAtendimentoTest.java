package br.com.superauto.crm.assistente.entidade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class ResumoDoAtendimentoTest {

    @Test
    @DisplayName("IA-02: resumo sem assunto ou longo demais é recusado, qualquer que seja o provedor")
    void formatoValidado() {
        assertThatThrownBy(() -> new ResumoDoAtendimento("ok", Set.of(), Satisfacao.NEUTRA, List.of(), List.of(), false,
                ChanceDeIndicar.BAIXA, null))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> new ResumoDoAtendimento("x".repeat(501), Set.of(Assunto.GUINCHO), Satisfacao.NEUTRA,
                List.of(), List.of(), false, ChanceDeIndicar.BAIXA, null))
                .isInstanceOf(NegocioException.class);
    }
}
