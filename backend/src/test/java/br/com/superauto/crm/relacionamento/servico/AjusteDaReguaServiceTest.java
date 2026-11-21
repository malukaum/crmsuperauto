package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.AjusteDaRegua;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFilaDeTeste;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class AjusteDaReguaServiceTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final List<EtapaRegua> REGUA = List.of(
            new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true),
            new EtapaRegua((short) 2, "D30", "D+30", 23, (short) 2, false));

    @Test
    @DisplayName("ATD-04: sem retorno manual, o ajuste conta o prazo da etapa a partir da base da régua do contrato")
    void usaBaseDaRegua() {
        var trabalho = mock(TrabalhoRelacionamentoRepository.class);
        var acompanhamentos = mock(AcompanhamentoContratoRepository.class);
        var regua = mock(ReguaDePosVenda.class);
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        // a base da régua do contrato de teste é 13/03/2026
        when(trabalho.findById(10L)).thenReturn(
                Optional.of(ContratoNaFilaDeTeste.contrato(10L, 7L, SituacaoNaCarteira.ATIVO, UUID.randomUUID())));
        when(acompanhamentos.buscarParaAlteracao(10L)).thenReturn(Optional.of(acompanhamento));
        when(regua.etapas()).thenReturn(REGUA);
        var relogio = Clock.fixed(LocalDate.of(2026, 3, 14).atStartOfDay(SAO_PAULO).toInstant(), SAO_PAULO);

        new AjusteDaReguaService(trabalho, acompanhamentos, regua, relogio)
                .ajustar(10L, new AjusteDaRegua(0, (short) 2, Optional.empty()), UUID.randomUUID());

        assertThat(acompanhamento.getEtapaPendenteId()).contains((short) 2);
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 4, 5));
        verify(acompanhamentos).save(acompanhamento);
    }
}
