package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.repositorio.ContagemDaCarteira;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class IndicadoresDaCarteiraTest {
    @Test
    @DisplayName(
            "FIL-01: carteira inclui suspensos e exclui cancelados; mensalidades somam somente"
                    + " ativos")
    void carteiraEMensalidades() {
        var contagens =
                List.of(
                        new ContagemDaCarteira(
                                "Carro", SituacaoNaCarteira.ATIVO, 2, new BigDecimal("200.00")),
                        new ContagemDaCarteira(
                                "Moto", SituacaoNaCarteira.A_CONFERIR, 1, new BigDecimal("50.00")),
                        new ContagemDaCarteira(
                                "Moto", SituacaoNaCarteira.SUSPENSO, 1, new BigDecimal("70.00")),
                        new ContagemDaCarteira(
                                "Carro",
                                SituacaoNaCarteira.CANCELADO,
                                10,
                                new BigDecimal("1000.00")));
        var totais = new IndicadoresDaCarteira().consolidar(contagens);
        assertThat(totais.carteira()).isEqualTo(4);
        assertThat(totais.porTipo()).containsEntry("Carro", 2L).containsEntry("Moto", 2L);
        assertThat(totais.mensalidadesAtivas()).isEqualByComparingTo("200.00");
        assertThat(totais.aConferir()).isEqualTo(1);
        assertThat(totais.situacoes()).containsEntry("CANCELADO", 10L);
    }

    @Test
    @DisplayName("FIL-01: carteira vazia tem indicadores zerados")
    void carteiraVazia() {
        var totais = new IndicadoresDaCarteira().consolidar(List.of());
        assertThat(totais.carteira()).isZero();
        assertThat(totais.mensalidadesAtivas()).isEqualByComparingTo("0");
    }
}
