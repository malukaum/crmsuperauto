package br.com.superauto.crm.assistente.infraestrutura.ia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MotivoDaFalhaTest {

    @Test
    @DisplayName("IA-05: conta sem crédito vira uma frase clara, sem o JSON do provedor")
    void semCredito() {
        String erro = "400: {\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Your credit "
                + "balance is too low to access the Anthropic API.\"},\"request_id\":\"req_1\"}";

        assertThat(MotivoDaFalha.de("Claude", 400, erro))
                .isEqualTo("Claude: sem crédito na conta do provedor. Confira o faturamento no site dele.")
                .doesNotContain("{");
    }

    @Test
    @DisplayName("IA-05: chave inválida e modelo inexistente são explicados ao gerencial")
    void chaveEModelo() {
        assertThat(MotivoDaFalha.de("Gemini", 401, "unauthorized")).isEqualTo("Gemini: chave de acesso inválida.");
        assertThat(MotivoDaFalha.de("ChatGPT", 404, "{}")).contains("modelo não encontrado");
    }

    @Test
    @DisplayName("IA-05: outro erro do pedido mostra só a mensagem do provedor, curta")
    void outroErro() {
        assertThat(MotivoDaFalha.de("Claude", 400, "{\"message\":\"" + "x".repeat(300) + "\"}"))
                .startsWith("Claude: o provedor recusou o pedido (400): x")
                .hasSizeLessThan(220);
    }
}
