package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.relacionamento.repositorio.ConsultaFilas;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

class ContadoresFilasServiceTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 31);

    @Test
    @DisplayName("FIL-02 / RG-01: o contador do menu usa o mesmo critério da fila e o hoje de São Paulo")
    void mesmoCriterioDaFila() {
        // 23h30 em São Paulo já é dia 1º em UTC: o contador precisa continuar em 31/10.
        var relogio = Clock.fixed(LocalDateTime.of(2026, 10, 31, 23, 30).atZone(SAO_PAULO).toInstant(), SAO_PAULO);
        var filas = mock(ConsultaFilas.class);
        var vendedor = Optional.of(UUID.randomUUID());
        when(filas.contarContatos(new ConsultaFilas.Contatos(false, HOJE, FiltroDaFila.SEM_FILTROS, vendedor)))
                .thenReturn(3L);
        when(filas.contarContatos(new ConsultaFilas.Contatos(true, HOJE, FiltroDaFila.SEM_FILTROS, vendedor)))
                .thenReturn(2L);

        var contadores = new ContadoresFilasService(filas, relogio).contar(vendedor);

        assertThat(contadores).isEqualTo(new ContadoresFilasService.Contadores(3, 2));
    }
}
