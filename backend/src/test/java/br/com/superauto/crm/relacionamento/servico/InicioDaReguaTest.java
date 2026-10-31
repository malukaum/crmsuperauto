package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.cadastro.entidade.ContratoCadastrado;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

class InicioDaReguaTest {

    private final AcompanhamentoContratoRepository acompanhamentos =
            mock(AcompanhamentoContratoRepository.class);
    private final ReguaDePosVenda regua = mock(ReguaDePosVenda.class);

    @Test
    @DisplayName(
            "CAR-02: ao cadastrar o contrato, ele entra na régua com D+7 pendente em adesão + 7"
                    + " dias")
    void abreARegua() {
        when(regua.etapas())
                .thenReturn(List.of(new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true)));
        UUID vendedor = UUID.randomUUID();

        new InicioDaRegua(acompanhamentos, regua)
                .aoCadastrarContrato(
                        new ContratoCadastrado(
                                9L,
                                5L,
                                6L,
                                vendedor,
                                Mensalidade.de("124.82"),
                                LocalDate.of(2026, 3, 13),
                                vendedor));

        var captor = ArgumentCaptor.forClass(AcompanhamentoContrato.class);
        verify(acompanhamentos).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(9L);
        assertThat(captor.getValue().getProximoContatoEm()).contains(LocalDate.of(2026, 3, 20));
    }
}
