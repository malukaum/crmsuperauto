package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.relacionamento.entidade.HistoricoExportado;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDoHistorico;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.ConsultaHistorico;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDoHistorico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class ConsultaHistoricoServiceTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final PeriodoDoHistorico OUTUBRO =
            new PeriodoDoHistorico(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    private static final FiltroDoHistorico SO_SUPORTE = new FiltroDoHistorico(Optional.of(TipoDeRelacionamento.SUPORTE),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    private final ConsultaHistorico historico = mock(ConsultaHistorico.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ConsultaHistoricoService servico =
            new ConsultaHistoricoService(historico, eventos, Clock.system(SAO_PAULO));

    @Test
    @DisplayName("ATD-05 / RG-01 / RG-03: o período vira dias inteiros de São Paulo e o vendedor só consulta a sua carteira")
    void periodoEEscopo() {
        var vendedor = Optional.of(UUID.randomUUID());
        when(historico.listar(any(), any())).thenReturn(Page.empty());

        servico.listar(new ConsultaDoHistorico(OUTUBRO, SO_SUPORTE), vendedor, PageRequest.of(0, 25));

        var pedido = ArgumentCaptor.forClass(ConsultaHistorico.Pedido.class);
        verify(historico).listar(pedido.capture(), any());
        assertThat(pedido.getValue().inicio()).isEqualTo(OffsetDateTime.parse("2026-10-01T00:00-03:00"));
        assertThat(pedido.getValue().fim()).isEqualTo(OffsetDateTime.parse("2026-11-01T00:00-03:00"));
        assertThat(pedido.getValue().vendedor()).isEqualTo(vendedor);
    }

    @Test
    @DisplayName("ATD-05 / CAR-07: a exportação vai para a auditoria com o filtro usado e a quantidade de linhas")
    void exportacaoAuditada() {
        when(historico.todos(any())).thenReturn(List.of());
        var autor = new AutorDoContato(UUID.randomUUID(), Optional.empty());

        servico.exportar(new ConsultaDoHistorico(OUTUBRO, SO_SUPORTE), autor);

        var evento = ArgumentCaptor.forClass(HistoricoExportado.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().filtro()).containsEntry("tipo", "SUPORTE").containsEntry("inicio", "2026-10-01");
        assertThat(evento.getValue().linhas()).isZero();
        assertThat(evento.getValue().autor()).isEqualTo(autor.id());
    }
}
