package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFilaDeTeste;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ConfiguracaoDosPrazos;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ConfiguracaoReguaServiceTest {
    private static final List<EtapaRegua> REGUA = List.of(
            new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true),
            new EtapaRegua((short) 2, "D30", "D+30", 23, (short) 2, false));
    private static final Map<Short, Integer> D7_EM_10_DIAS = Map.of((short) 1, 10, (short) 2, 23);

    private final ReguaDePosVenda regua = mock(ReguaDePosVenda.class);
    private final ConfiguracaoDosPrazos configuracao = mock(ConfiguracaoDosPrazos.class);
    private final TrabalhoRelacionamentoRepository trabalho = mock(TrabalhoRelacionamentoRepository.class);
    private final AcompanhamentoContratoRepository acompanhamentos = mock(AcompanhamentoContratoRepository.class);
    private final ConfiguracaoReguaService servico =
            new ConfiguracaoReguaService(regua, configuracao, trabalho, acompanhamentos);
    private AcompanhamentoContrato acompanhamento;

    @BeforeEach
    void preparar() {
        // adesão (base da régua) em 13/03/2026, D+7 pendente para 20/03
        acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        when(regua.etapas()).thenReturn(REGUA);
        when(trabalho.comReguaAtiva()).thenReturn(
                List.of(ContratoNaFilaDeTeste.contrato(10L, 7L, SituacaoNaCarteira.ATIVO, UUID.randomUUID())));
        when(acompanhamentos.findAllById(any())).thenReturn(List.of(acompanhamento));
        when(acompanhamentos.buscarTodosParaAlteracao(any())).thenReturn(List.of(acompanhamento));
    }

    @Test
    @DisplayName("FIL-06: a prévia conta os contratos que mudam de data, e salvar com ela recalcula a régua")
    void previaESalvar() {
        var previa = servico.prever(D7_EM_10_DIAS);

        servico.salvar(D7_EM_10_DIAS, previa.confirmacao(), UUID.randomUUID());

        assertThat(previa.contratosAfetados()).isEqualTo(1);
        verify(acompanhamentos).buscarTodosParaAlteracao(any());
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 3, 23));
        verify(configuracao).alterarPrazos(any(), any());
    }

    @Test
    @DisplayName("FIL-06: se a carteira ou a régua mudou depois da prévia, salvar exige uma nova prévia")
    void previaDesatualizada() {
        assertThatThrownBy(() -> servico.salvar(D7_EM_10_DIAS, "previa-antiga", UUID.randomUUID()))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("previa-desatualizada");
        verify(configuracao, never()).alterarPrazos(any(), any());
    }
}
