package br.com.superauto.crm.compartilhado.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelefoneTest {

    @Test
    @DisplayName("CAR-04: \"(37) 99869-2950\" é gravado como 5537998692950 e exibido como (37) 9 9869-2950")
    void criterioDeAceite() {
        var telefone = Telefone.de("(37) 99869-2950");

        assertThat(telefone.valor()).isEqualTo("5537998692950");
        assertThat(telefone.formatado()).isEqualTo("(37) 9 9869-2950");
    }

    @ParameterizedTest
    @ValueSource(strings = {"+55 37 99869-2950", "55 (37) 9 9869-2950", "37998692950", "037-99869-2950"})
    @DisplayName("Aceita o número com ou sem 55, parênteses, espaços e traços")
    void aceitaVariasFormas(String digitado) {
        assertThat(Telefone.de(digitado).valor()).isEqualTo("5537998692950");
    }

    @Test
    @DisplayName("Fixo com DDD é aceito e exibido sem o nono dígito")
    void fixo() {
        var telefone = Telefone.de("(37) 3321-1234");

        assertThat(telefone.valor()).isEqualTo("553733211234");
        assertThat(telefone.celular()).isFalse();
        assertThat(telefone.formatado()).isEqualTo("(37) 3321-1234");
    }

    @Test
    @DisplayName("Com 10 ou 11 dígitos começando por 55, o 55 é o DDD (Rio Grande do Sul)")
    void ddd55() {
        assertThat(Telefone.de("55 99999-8888").valor()).isEqualTo("5555999998888");
    }

    @ParameterizedTest
    @ValueSource(strings = {"99869-2950", "998692950", "", "abc", "5537998692950123"})
    @DisplayName("Sem DDD ou com dígitos de menos ou de mais é recusado")
    void dddObrigatorio(String digitado) {
        assertThatThrownBy(() -> Telefone.de(digitado))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("telefone-invalido"));
    }
}
