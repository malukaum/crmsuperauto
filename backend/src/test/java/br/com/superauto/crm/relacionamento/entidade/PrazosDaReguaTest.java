package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.*;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

class PrazosDaReguaTest {
    private static final EtapaRegua D7 = new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true);
    private static final EtapaRegua D30 =
            new EtapaRegua((short) 2, "D30", "D+30", 23, (short) 2, false);

    @Test
    @DisplayName("FIL-06: aceita os limites de 1 e 365 dias e mantém ordem e sequência")
    void limitesValidos() {
        var etapas =
                new PrazosDaRegua(Map.of((short) 1, 1, (short) 2, 365)).aplicar(List.of(D7, D30));
        assertThat(etapas).extracting(EtapaRegua::diasAposAnterior).containsExactly(1, 365);
        assertThat(etapas)
                .extracting(EtapaRegua::proximaEtapaId)
                .containsExactly((short) 2, (short) 2);
    }

    @Test
    @DisplayName("FIL-06: recusa prazos abaixo de 1 e acima de 365 dias")
    void limitesInvalidos() {
        for (int prazo : List.of(0, 366))
            assertThatThrownBy(() -> new PrazosDaRegua(Map.of((short) 1, prazo)))
                    .isInstanceOf(NegocioException.class);
    }

    @Test
    @DisplayName("FIL-06: não permite criar ou omitir etapas")
    void etapasFixas() {
        assertThatThrownBy(() -> new PrazosDaRegua(Map.of((short) 1, 7)).aplicar(List.of(D7, D30)))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(
                        () ->
                                new PrazosDaRegua(
                                                Map.of((short) 1, 7, (short) 2, 23, (short) 3, 30))
                                        .aplicar(List.of(D7, D30)))
                .isInstanceOf(NegocioException.class);
    }
}
