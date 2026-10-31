package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AcompanhamentoContratoTest {

    static final EtapaRegua D7 = new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true);
    static final EtapaRegua D30 = new EtapaRegua((short) 2, "D30", "D+30", 23, (short) 3, false);
    static final EtapaRegua D60 = new EtapaRegua((short) 3, "D60", "D+60", 30, (short) 4, false);
    static final EtapaRegua D90 = new EtapaRegua((short) 4, "D90", "D+90", 30, (short) 5, false);
    static final EtapaRegua TRIMESTRAL = new EtapaRegua((short) 5, "TRIMESTRAL", "Trimestral", 90, (short) 5, false);
    static final List<EtapaRegua> REGUA = List.of(D7, D30, D60, D90, TRIMESTRAL);

    @Test
    @DisplayName("CAR-02: dada a adesão em 13/03/2026, o próximo contato é 20/03/2026 e a etapa pendente é D+7")
    void criterioDeAceite() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));

        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 3, 20));
        assertThat(acompanhamento.getEtapaPendenteId()).contains(D7.id());
        assertThat(acompanhamento.getId()).isEqualTo(10L);
        assertThat(acompanhamento.isNew()).isTrue();
    }

    @Test
    @DisplayName("A etapa inicial é a marcada como inicial, qualquer que seja a posição na lista")
    void usaAEtapaInicial() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, List.of(D30, D7), LocalDate.of(2026, 3, 13));

        assertThat(acompanhamento.getEtapaPendenteId()).contains(D7.id());
    }
}
