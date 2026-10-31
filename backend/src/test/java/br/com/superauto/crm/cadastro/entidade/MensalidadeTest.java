package br.com.superauto.crm.cadastro.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MensalidadeTest {

    @Test
    @DisplayName("Guarda sempre com duas casas")
    void duasCasas() {
        assertThat(Mensalidade.de("120").valor()).isEqualByComparingTo("120.00").hasScaleOf(2);
        assertThat(Mensalidade.de("120.5")).isEqualTo(Mensalidade.de("120.50"));
    }

    @Test
    @DisplayName("Recusa valor negativo e frações de centavo")
    void recusaInvalidos() {
        assertThatThrownBy(() -> Mensalidade.de("-1")).hasMessageContaining("negativa");
        assertThatThrownBy(() -> new Mensalidade(new BigDecimal("10.001"))).hasMessageContaining("dois decimais");
    }
}
