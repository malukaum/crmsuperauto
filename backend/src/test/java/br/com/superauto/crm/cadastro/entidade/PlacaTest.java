package br.com.superauto.crm.cadastro.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlacaTest {

    @Test
    @DisplayName("RG-02: \"haa-7216\" vira HAA7216, em maiúsculas e sem hífen")
    void normaliza() {
        assertThat(Placa.de("haa-7216").valor()).isEqualTo("HAA7216");
    }

    @Test
    @DisplayName("Aceita o padrão Mercosul")
    void mercosul() {
        assertThat(Placa.de("bra2e19").valor()).isEqualTo("BRA2E19");
    }

    @ParameterizedTest
    @ValueSource(strings = {"HA7216", "HAAA216", "1AA7216", "HAA72E6", ""})
    @DisplayName("Fora do padrão antigo ou Mercosul é recusada")
    void invalida(String digitada) {
        assertThatThrownBy(() -> Placa.de(digitada))
                .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo("placa-invalida"));
    }
}
