package br.com.superauto.crm.cadastro.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContratoTest {

    private static final OffsetDateTime ADESAO = OffsetDateTime.parse("2026-10-01T10:00:00-03:00");

    private final UUID vendedor = UUID.randomUUID();
    private final UUID gerente = UUID.randomUUID();

    @Test
    @DisplayName("Contrato novo nasce a conferir, manual e vigente (RG-12)")
    void novo() {
        var contrato = novoContrato();

        assertThat(contrato.getSituacao()).isEqualTo(SituacaoContrato.A_CONFERIR);
        assertThat(contrato.getOrigem()).isEqualTo(OrigemContrato.MANUAL);
        assertThat(contrato.vigente()).isTrue();
    }

    @Test
    @DisplayName("Transferir troca o vendedor e registra o evento com o anterior, o novo e o autor")
    void transferir() {
        var contrato = novoContrato();
        contrato.transferirPara(vendedor, gerente);
        UUID outro = UUID.randomUUID();

        contrato.transferirPara(outro, gerente);

        assertThat(contrato.getVendedorId()).isEqualTo(outro);
        assertThat(contrato.eventosPendentes()).last()
                .isEqualTo(new ContratoTransferido(null, vendedor, outro, gerente));
    }

    @Test
    @DisplayName("Contrato cancelado não muda de vendedor e fica com o original (RG-07)")
    void canceladoNaoTransfere() {
        var contrato = novoContrato();
        contrato.transferirPara(vendedor, gerente);
        contrato.cancelar(ADESAO.plusMonths(1));

        assertThatThrownBy(() -> contrato.transferirPara(UUID.randomUUID(), gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("contrato-cancelado"));
        assertThat(contrato.getVendedorId()).isEqualTo(vendedor);
        assertThat(contrato.getEncerradoEm()).isEqualTo(ADESAO.plusMonths(1));
    }

    private static Contrato novoContrato() {
        return Contrato.novo(new Adesao(1L, 2L, Mensalidade.de("99.90"), ADESAO));
    }
}
