package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import br.com.superauto.crm.relacionamento.entidade.EtapaNaRegua;
import br.com.superauto.crm.relacionamento.entidade.EtapaNaRegua.Andamento;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrevisaoReguaTest {

    private static final EtapaRegua D7 = new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true);
    private static final EtapaRegua D30 = new EtapaRegua((short) 2, "D30", "D+30", 23, (short) 3, false);
    private static final EtapaRegua D60 = new EtapaRegua((short) 3, "D60", "D+60", 30, (short) 4, false);
    private static final List<EtapaRegua> REGUA = List.of(D7, D30, D60);

    @Test
    @DisplayName("CAR-03: contrato novo tem o D+7 pendente na data do próximo contato e as futuras somando os prazos")
    void contratoNovo() {
        var previsao = PrevisaoRegua.prever(REGUA, Optional.of(D7.id()), Optional.of(LocalDate.of(2026, 3, 20)));

        assertThat(previsao).extracting(EtapaNaRegua::andamento)
                .containsExactly(Andamento.PENDENTE, Andamento.FUTURA, Andamento.FUTURA);
        assertThat(previsao).extracting(EtapaNaRegua::prevista).containsExactly(
                Optional.of(LocalDate.of(2026, 3, 20)), Optional.of(LocalDate.of(2026, 4, 12)),
                Optional.of(LocalDate.of(2026, 5, 12)));
    }

    @Test
    @DisplayName("CAR-03: as etapas antes da pendente aparecem concluídas, sem data prevista")
    void concluidas() {
        var previsao = PrevisaoRegua.prever(REGUA, Optional.of(D60.id()), Optional.of(LocalDate.of(2026, 6, 1)));

        assertThat(previsao).extracting(EtapaNaRegua::andamento)
                .containsExactly(Andamento.CONCLUIDA, Andamento.CONCLUIDA, Andamento.PENDENTE);
        assertThat(previsao.get(0).prevista()).isEmpty();
    }

    @Test
    @DisplayName("Régua encerrada fica toda concluída")
    void encerrada() {
        assertThat(PrevisaoRegua.prever(REGUA, Optional.empty(), Optional.empty()))
                .extracting(EtapaNaRegua::andamento).containsOnly(Andamento.CONCLUIDA);
    }

    @Test
    @DisplayName("Sem data de próximo contato, a pendente e as futuras ficam sem data")
    void semData() {
        assertThat(PrevisaoRegua.prever(REGUA, Optional.of(D7.id()), Optional.empty()))
                .extracting(EtapaNaRegua::prevista).containsOnly(Optional.empty());
    }
}
