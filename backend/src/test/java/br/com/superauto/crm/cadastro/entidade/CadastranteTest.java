package br.com.superauto.crm.cadastro.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CadastranteTest {

    private final UUID autor = UUID.randomUUID();
    private final UUID outroVendedor = UUID.randomUUID();

    @Test
    @DisplayName("CAR-02: o vendedor do contrato é o vendedor que cadastra, mesmo que indique outro")
    void vendedorCadastraParaSi() {
        assertThat(new Cadastrante(autor, false).vendedorDoContrato(Optional.of(outroVendedor))).isEqualTo(autor);
    }

    @Test
    @DisplayName("CAR-02: o gerencial escolhe o vendedor")
    void gerencialEscolhe() {
        assertThat(new Cadastrante(autor, true).vendedorDoContrato(Optional.of(outroVendedor))).isEqualTo(outroVendedor);
    }

    @Test
    @DisplayName("CAR-02: sem escolher o vendedor, o gerencial não cadastra")
    void gerencialPrecisaEscolher() {
        assertThatThrownBy(() -> new Cadastrante(autor, true).vendedorDoContrato(Optional.empty()))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("vendedor-obrigatorio"));
    }
}
