package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProximaAcaoTest {

    @Test
    @DisplayName("ATD-05: no pós-venda, Concluído é retorno automático; Sem resposta e Reagendado, nova tentativa")
    void posVendaComoNaMacro() {
        assertThat(ProximaAcao.de(TipoDeRelacionamento.POS_VENDA, ResultadoContato.CONCLUIDO).descricao())
                .isEqualTo("Retorno automático");
        assertThat(ProximaAcao.de(TipoDeRelacionamento.POS_VENDA, ResultadoContato.SEM_RESPOSTA))
                .isEqualTo(ProximaAcao.NOVA_TENTATIVA);
        assertThat(ProximaAcao.de(TipoDeRelacionamento.POS_VENDA, ResultadoContato.REAGENDADO))
                .isEqualTo(ProximaAcao.NOVA_TENTATIVA);
    }

    @Test
    @DisplayName("ATD-05: aniversário e avulsos não alteram o retorno, qualquer que seja o resultado")
    void semAlteracaoForaDoPosVenda() {
        assertThat(ProximaAcao.de(TipoDeRelacionamento.ANIVERSARIO, ResultadoContato.CONCLUIDO).descricao())
                .isEqualTo("Sem alteração no retorno");
        assertThat(ProximaAcao.de(TipoDeRelacionamento.SUPORTE, ResultadoContato.REAGENDADO))
                .isEqualTo(ProximaAcao.SEM_ALTERACAO);
    }
}
