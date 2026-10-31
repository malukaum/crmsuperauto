package br.com.superauto.crm.auditoria.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.OperacaoAuditoria;
import br.com.superauto.crm.cadastro.entidade.ContratoCadastrado;
import br.com.superauto.crm.cadastro.entidade.ContratoTransferido;
import br.com.superauto.crm.cadastro.entidade.MensalidadeReajustada;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.entidade.SituacaoContratoAlterada;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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

    @Test
    @DisplayName("CAR-05: reajuste de R$ 124,82 para R$ 139,90 fica na auditoria com os dois valores e quem alterou")
    void reajuste() {
        UUID gerente = UUID.randomUUID();

        auditoria.aoReajustar(new MensalidadeReajustada(42L, Mensalidade.de("124.82"), Mensalidade.de("139.90"), gerente));

        var evento = registrado();
        assertThat(evento.getOperacao()).isEqualTo(OperacaoAuditoria.ALTERACAO);
        assertThat(evento.getUsuarioId()).isEqualTo(gerente);
        assertThat(evento.getDadosAnteriores()).isEqualTo(Map.of("mensalidade", "124.82"));
        assertThat(evento.getDadosNovos()).isEqualTo(Map.of("mensalidade", "139.90"));
    }

    @Test
    @DisplayName("CAR-05: mudança de situação registra a anterior e a nova")
    void situacao() {
        auditoria.aoAlterarSituacao(new SituacaoContratoAlterada(42L, SituacaoContrato.ATIVO, SituacaoContrato.CANCELADO,
                UUID.randomUUID()));

        assertThat(registrado().getDadosNovos()).isEqualTo(Map.of("situacao", "CANCELADO"));
    }

    @Test
    @DisplayName("CAR-02: contrato cadastrado vira evento de criação com vendedor, mensalidade e adesão")
    void cadastro() {
        UUID vendedor = UUID.randomUUID();

        auditoria.aoCadastrar(new ContratoCadastrado(42L, 1L, 2L, vendedor, Mensalidade.de("99.90"),
                LocalDate.of(2026, 3, 13), vendedor));

        var evento = registrado();
        assertThat(evento.getOperacao()).isEqualTo(OperacaoAuditoria.CRIACAO);
        assertThat(evento.getDadosNovos()).containsEntry("mensalidade", "99.90").containsEntry("dataAdesao", "2026-03-13");
    }

    private EventoAuditoria registrado() {
        var captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(registro).registrar(captor.capture());
        return captor.getValue();
    }
}
