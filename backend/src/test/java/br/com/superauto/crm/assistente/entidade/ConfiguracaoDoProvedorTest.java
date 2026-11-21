package br.com.superauto.crm.assistente.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

class ConfiguracaoDoProvedorTest {

    private static AjusteDoProvedor ajuste(boolean ativo, String modelo) {
        return new AjusteDoProvedor(0, modelo, ativo, new BigDecimal("20"), Optional.of(new BigDecimal("4")),
                Optional.of(new BigDecimal("20")));
    }

    @Test
    @DisplayName("IA-05: o custo estimado usa os preços por milhão de tokens de entrada e de saída")
    void custoEstimado() {
        var claude = ConfiguracoesDeTeste.provedor(ProvedorDeIa.CLAUDE, true, "claude-opus-5-5", "x", BigDecimal.TEN);

        assertThat(claude.custoDe(2_000, 400)).hasValueSatisfying(
                custo -> assertThat(custo).isEqualByComparingTo("0.016"));
    }

    @Test
    @DisplayName("IA-05: ligar o provedor exige modelo e chave")
    void ligarExigeModeloEChave() {
        var gemini = ConfiguracoesDeTeste.provedor(ProvedorDeIa.GEMINI, false, null, null, BigDecimal.TEN);

        assertThatThrownBy(() -> gemini.ajustar(ajuste(true, "gemini-modelo"), Optional.empty(), UUID.randomUUID()))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("provedor-incompleto");
    }

    @Test
    @DisplayName("IA-05: com chave nova e modelo, o provedor liga e fica pronto para uso; a chave cifrada é a guardada")
    void ligaComChave() {
        var gemini = ConfiguracoesDeTeste.provedor(ProvedorDeIa.GEMINI, false, null, null, BigDecimal.TEN);

        gemini.ajustar(ajuste(true, "gemini-modelo"),
                Optional.of(new ConfiguracaoDoProvedor.ChaveCifrada("cifrada", "abcd")), UUID.randomUUID());

        assertThat(gemini.prontoParaUso()).isTrue();
        assertThat(gemini.getChaveCifrada()).contains("cifrada");
        assertThat(gemini.getChaveFinal()).contains("abcd");
    }

    @Test
    @DisplayName("IA-05: atingido o limite do mês, o provedor deixa de ser usado")
    void limiteDoMes() {
        var claude = ConfiguracoesDeTeste.provedor(ProvedorDeIa.CLAUDE, true, "m", "x", new BigDecimal("20"));

        assertThat(claude.dentroDoLimite(new BigDecimal("19.99"))).isTrue();
        assertThat(claude.dentroDoLimite(new BigDecimal("20"))).isFalse();
    }
}
