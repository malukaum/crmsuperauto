package br.com.superauto.crm.auditoria.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.auditoria.entidade.Alteracao;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;
import br.com.superauto.crm.auditoria.entidade.OperacaoAuditoria;
import br.com.superauto.crm.auditoria.repositorio.EventoAuditoriaRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistroAuditoriaTest {

    private final EventoAuditoriaRepository eventos = mock(EventoAuditoriaRepository.class);
    private final RegistroAuditoria registro = new RegistroAuditoria(eventos);

    @Test
    @DisplayName("Grava o evento com autor e os dados anteriores e novos (aceita valores nulos)")
    void grava() {
        UUID autor = UUID.randomUUID();
        Map<String, Object> novo = new HashMap<>();
        novo.put("email", null);
        var evento = EventoAuditoria.alteracao(ItemAuditado.de("usuario", 42), autor,
                new Alteracao(Map.of("email", "a@x.com"), novo));

        registro.registrar(evento);

        verify(eventos).save(evento);
        assertThat(evento.getEntidadeId()).isEqualTo("42");
        assertThat(evento.getOperacao()).isEqualTo(OperacaoAuditoria.ALTERACAO);
        assertThat(evento.getUsuarioId()).isEqualTo(autor);
        assertThat(evento.getDadosAnteriores()).containsEntry("email", "a@x.com");
        assertThat(evento.getDadosNovos()).containsEntry("email", null);
    }

    @Test
    @DisplayName("Criação não tem dados anteriores")
    void criacao() {
        var evento = EventoAuditoria.criacao(ItemAuditado.de("usuario", 1), UUID.randomUUID(), Map.of("ativo", true));

        assertThat(evento.getOperacao()).isEqualTo(OperacaoAuditoria.CRIACAO);
        assertThat(evento.getDadosAnteriores()).isNull();
        assertThat(evento.getDadosNovos()).containsEntry("ativo", true);
    }
}
