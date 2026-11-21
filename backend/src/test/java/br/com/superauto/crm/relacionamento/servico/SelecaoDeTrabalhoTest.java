package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.relacionamento.entidade.ContratoNaFilaDeTeste;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDeAniversarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

class SelecaoDeTrabalhoTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 31);
    private final SelecaoDeTrabalho selecao = new SelecaoDeTrabalho();

    @Test
    @DisplayName("FIL-04: cliente com duas placas que faz aniversário hoje aparece uma única vez")
    void umaLinhaPorPessoa() {
        var nascimento = LocalDate.of(1992, 10, 31);
        var contratos = List.of(
                ContratoNaFilaDeTeste.aniversariante(10L, 7L, "HAA7216", nascimento),
                ContratoNaFilaDeTeste.aniversariante(11L, 7L, "BRA2E19", nascimento));

        var linhas = selecao.aniversarios(contratos, new PeriodoDeAniversarios(HOJE, 0), Set.of());

        assertThat(linhas).singleElement().satisfies(linha -> {
            assertThat(linha.placa()).isEqualTo("HAA7216 / BRA2E19");
            assertThat(linha.mensalidade()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(linha.data()).isEqualTo(HOJE);
        });
    }

    @Test
    @DisplayName("ATD-03: depois de registrada a mensagem, a linha mostra \"Mensagem enviada\"")
    void mensagemEnviada() {
        var contratos = List.of(ContratoNaFilaDeTeste.aniversariante(10L, 7L, "HAA7216", LocalDate.of(1992, 10, 31)));

        var linhas = selecao.aniversarios(contratos, new PeriodoDeAniversarios(HOJE, 0), Set.of(7L));

        assertThat(linhas).singleElement().satisfies(linha -> assertThat(linha.mensagemEnviada()).isTrue());
    }
}
