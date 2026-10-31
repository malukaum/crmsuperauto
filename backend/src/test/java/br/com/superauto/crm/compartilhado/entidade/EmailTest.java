package br.com.superauto.crm.compartilhado.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    @DisplayName("Normaliza espaços e maiúsculas: e-mails iguais são o mesmo valor")
    void normaliza() {
        assertThat(Email.de("  Maria@SuperAuto.LOCAL ")).isEqualTo(Email.de("maria@superauto.local"));
    }

    @Test
    @DisplayName("Recusa e-mail fora do formato")
    void recusaInvalido() {
        assertThatThrownBy(() -> Email.de("maria")).isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo("email-invalido"));
        assertThatThrownBy(() -> Email.de("a@" + "x".repeat(Email.TAMANHO_MAXIMO) + ".com"))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    @DisplayName("Dado externo ausente ou inválido vira vazio, sem lançar")
    void seValido() {
        assertThat(Email.seValido(null)).isEmpty();
        assertThat(Email.seValido(" ")).isEmpty();
        assertThat(Email.seValido("sem-arroba")).isEmpty();
        assertThat(Email.seValido("a@x.com")).contains(Email.de("a@x.com"));
    }
}
