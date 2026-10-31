package br.com.superauto.crm.cadastro.entidade;

import static br.com.superauto.crm.cadastro.entidade.SituacaoContrato.ATIVO;
import static br.com.superauto.crm.cadastro.entidade.SituacaoContrato.A_CONFERIR;
import static br.com.superauto.crm.cadastro.entidade.SituacaoContrato.CANCELADO;
import static br.com.superauto.crm.cadastro.entidade.SituacaoContrato.SUSPENSO;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SituacaoContratoTest {

    @Test
    @DisplayName("CAR-05: de \"A conferir\" vai para Ativo, Suspenso ou Cancelado")
    void aConferir() {
        assertThat(A_CONFERIR.podeMudarPara(ATIVO)).isTrue();
        assertThat(A_CONFERIR.podeMudarPara(SUSPENSO)).isTrue();
        assertThat(A_CONFERIR.podeMudarPara(CANCELADO)).isTrue();
    }

    @Test
    @DisplayName("CAR-05: Ativo e Suspenso vão um para o outro, mas não voltam para \"A conferir\"")
    void ativoESuspenso() {
        assertThat(ATIVO.podeMudarPara(SUSPENSO)).isTrue();
        assertThat(SUSPENSO.podeMudarPara(ATIVO)).isTrue();
        assertThat(ATIVO.podeMudarPara(A_CONFERIR)).isFalse();
        assertThat(SUSPENSO.podeMudarPara(A_CONFERIR)).isFalse();
    }

    @Test
    @DisplayName("CAR-05: Cancelado é definitivo (proposta)")
    void canceladoDefinitivo() {
        for (SituacaoContrato destino : SituacaoContrato.values()) {
            assertThat(CANCELADO.podeMudarPara(destino)).isFalse();
        }
    }
}
