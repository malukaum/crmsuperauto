package br.com.superauto.crm.auditoria.servico;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.OperacaoAuditoria;
import br.com.superauto.crm.referencia.entidade.PrazoDaEtapaAlterado;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.AtendimentoRegistrado;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.UUID;

class AuditoriaDeRelacionamentoTest {
    @Test
    @DisplayName("FIL-06: auditoria guarda autor e prazos anterior e novo")
    void alterarPrazo() {
        var registro = mock(RegistroAuditoria.class);
        var auditoria = new AuditoriaDeRelacionamento(registro);
        var autor = UUID.randomUUID();
        auditoria.aoAlterarPrazo(new PrazoDaEtapaAlterado((short) 2, 23, 25, autor));
        var evento = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(registro).registrar(evento.capture());
        assertThat(evento.getValue().getUsuarioId()).isEqualTo(autor);
        assertThat(evento.getValue().getDadosAnteriores()).containsEntry("diasAposAnterior", 23);
        assertThat(evento.getValue().getDadosNovos()).containsEntry("diasAposAnterior", 25);
    }

    @Test
    @DisplayName("FIL-02: novo atendimento produz auditoria de criação com seus dados")
    void registrarContato() {
        var registro = mock(RegistroAuditoria.class);
        var auditoria = new AuditoriaDeRelacionamento(registro);
        auditoria.aoRegistrarAtendimento(
                new AtendimentoRegistrado(
                        10L,
                        new Atendimento.Novo(
                                7L,
                                5L,
                                (short) 1,
                                (short) 1,
                                ResultadoContato.CONCLUIDO,
                                null,
                                null,
                                OffsetDateTime.parse("2026-10-31T12:00:00-03:00"),
                                UUID.randomUUID())));
        var evento = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(registro).registrar(evento.capture());
        assertThat(evento.getValue().getOperacao()).isEqualTo(OperacaoAuditoria.CRIACAO);
        assertThat(evento.getValue().getDadosNovos())
                .containsEntry("resultado", "CONCLUIDO")
                .containsEntry("associadoId", 7L);
    }
}
