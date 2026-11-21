package br.com.superauto.crm.assistente.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.assistente.entidade.Assunto;
import br.com.superauto.crm.assistente.entidade.Satisfacao;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class InterpretacaoDasRespostasTest {
    private final InterpretacaoDasRespostas interpretacao = new InterpretacaoDasRespostas(new Pseudonimizacao());

    @Test
    @DisplayName("IA-02: o resumo em JSON (mesmo dentro de ```json) vira o resumo do domínio, com os marcadores restaurados")
    void resumo() {
        String resposta = """
                ```json
                {"resumoCurto": "[CLIENTE] reclamou do guincho", "assuntos": ["GUINCHO", "INEXISTENTE"],
                 "satisfacao": "negativa", "objecoes": ["Demora"], "interesses": [], "riscoDeCancelamento": true,
                 "chanceDeIndicar": "BAIXA", "proximaAcao": "Ligar para [CLIENTE]"}
                ```""";

        var resumo = interpretacao.resumo(resposta, Map.of("[CLIENTE]", "José"));

        assertThat(resumo.resumoCurto()).isEqualTo("José reclamou do guincho");
        assertThat(resumo.assuntos()).containsExactlyInAnyOrder(Assunto.GUINCHO, Assunto.OUTRO);
        assertThat(resumo.satisfacao()).isEqualTo(Satisfacao.NEGATIVA);
        assertThat(resumo.riscoDeCancelamento()).isTrue();
    }

    @Test
    @DisplayName("IA-02: resposta fora do formato é recusada, para o assistente tentar de novo")
    void foraDoFormato() {
        assertThatThrownBy(() -> interpretacao.resumo("Não consigo ajudar com isso.", Map.of()))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("resposta-invalida");
    }

    @Test
    @DisplayName("IA-04: no máximo 3 recomendações são aproveitadas")
    void recomendacoes() {
        String resposta = """
                {"recomendacoes": [{"abordagem": "a", "base": "x"}, {"abordagem": "b", "base": "x"},
                                   {"abordagem": "c", "base": "x"}, {"abordagem": "d", "base": "x"}]}""";

        assertThat(interpretacao.recomendacoes(resposta, Map.of())).hasSize(3);
    }
}
