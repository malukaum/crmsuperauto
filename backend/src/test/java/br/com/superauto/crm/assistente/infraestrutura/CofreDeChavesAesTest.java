package br.com.superauto.crm.assistente.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CofreDeChavesAesTest {

    @Test
    @DisplayName("IA-05: a chave guardada é ilegível e volta igual só com a mesma chave-mestra")
    void cifraEDecifra() {
        var cofre = new CofreDeChavesAes("mestra");

        String cifrada = cofre.cifrar("sk-segredo-1234");

        assertThat(cifrada).doesNotContain("segredo");
        assertThat(cofre.decifrar(cifrada)).isEqualTo("sk-segredo-1234");
        assertThatThrownBy(() -> new CofreDeChavesAes("outra").decifrar(cifrada)).isInstanceOf(IllegalStateException.class);
    }
}
