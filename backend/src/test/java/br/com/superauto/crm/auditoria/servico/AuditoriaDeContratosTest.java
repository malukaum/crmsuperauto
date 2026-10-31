package br.com.superauto.crm.auditoria.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.OperacaoAuditoria;
import br.com.superauto.crm.cadastro.entidade.ContratoTransferido;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditoriaDeContratosTest {

    private final RegistroAuditoria registro = mock(RegistroAuditoria.class);
    private final AuditoriaDeContratos auditoria = new AuditoriaDeContratos(registro);

    @Test
    @DisplayName("Transferência de contrato vira evento de auditoria com vendedor anterior e novo (RG-08)")
    void transferencia() {
        UUID anterior = UUID.randomUUID();
        UUID novo = UUID.randomUUID();
        UUID autor = UUID.randomUUID();

        auditoria.aoTransferir(new ContratoTransferido(42L, anterior, novo, autor));

        var evento = registrado();
        assertThat(evento.getEntidade()).isEqualTo("contrato");
        assertThat(evento.getEntidadeId()).isEqualTo("42");
        assertThat(evento.getOperacao()).isEqualTo(OperacaoAuditoria.ALTERACAO);
        assertThat(evento.getUsuarioId()).isEqualTo(autor);
        assertThat(evento.getDadosAnteriores()).isEqualTo(Map.of("vendedorId", anterior.toString()));
        assertThat(evento.getDadosNovos()).isEqualTo(Map.of("vendedorId", novo.toString()));
    }

    @Test
    @DisplayName("Contrato que não tinha vendedor registra o anterior como nulo")
    void semVendedorAnterior() {
        auditoria.aoTransferir(new ContratoTransferido(7L, null, UUID.randomUUID(), UUID.randomUUID()));

        assertThat(registrado().getDadosAnteriores()).containsEntry("vendedorId", null);
    }

    private EventoAuditoria registrado() {
        var captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(registro).registrar(captor.capture());
        return captor.getValue();
    }
}
