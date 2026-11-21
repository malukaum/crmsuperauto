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
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class RegistroDeAtendimentoServiceTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO =
            Clock.fixed(LocalDateTime.of(2026, 3, 25, 12, 0).atZone(SAO_PAULO).toInstant(), SAO_PAULO);
    private static final List<EtapaRegua> REGUA = List.of(
            new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true),
            new EtapaRegua((short) 2, "D30", "D+30", 23, (short) 2, false));
    private static final AutorDoContato AUTOR = new AutorDoContato(UUID.randomUUID(), Optional.empty());

    private final CarteiraVisivel carteira = mock(CarteiraVisivel.class);
    private final AcompanhamentoContratoRepository acompanhamentos = mock(AcompanhamentoContratoRepository.class);
    private final AtendimentoRepository atendimentos = mock(AtendimentoRepository.class);
    private final ReguaDePosVenda regua = mock(ReguaDePosVenda.class);
    private final TiposDeContato tipos = mock(TiposDeContato.class);
    private final RegistroDeAtendimentoService servico =
            new RegistroDeAtendimentoService(carteira, acompanhamentos, atendimentos, regua, tipos, RELOGIO);
    private AcompanhamentoContrato acompanhamento;

    @BeforeEach
    void preparar() {
        acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        when(acompanhamentos.findById(10L)).thenReturn(Optional.of(acompanhamento));
        when(acompanhamentos.buscarParaAlteracao(10L)).thenReturn(Optional.of(acompanhamento));
        when(regua.etapas()).thenReturn(REGUA);
        when(tipos.id(TipoDeRelacionamento.POS_VENDA)).thenReturn((short) 1);
    }

    private void contratoNaSituacao(SituacaoNaCarteira situacao) {
        when(carteira.contrato(10L, Optional.empty()))
                .thenReturn(ContratoNaFilaDeTeste.contrato(10L, 7L, situacao, UUID.randomUUID()));
    }

    private static RegistroDeContato concluido(OrigemDoRegistro origem) {
        return new RegistroDeContato(
                0, ResultadoContato.CONCLUIDO, Optional.empty(), null, Optional.empty(), origem);
    }

    @Test
    @DisplayName("ATD-01: pela ficha, contrato suspenso pode ser registrado, como na macro")
    void suspensoPelaFicha() {
        contratoNaSituacao(SituacaoNaCarteira.SUSPENSO);

        servico.registrar(10L, concluido(OrigemDoRegistro.FICHA), AUTOR);

        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 4, 17));
        verify(atendimentos).save(any());
    }

    @Test
    @DisplayName("RG-06: o registro trava o acompanhamento, e registros simultâneos do contrato entram em fila")
    void registroComTrava() {
        contratoNaSituacao(SituacaoNaCarteira.ATIVO);

        servico.registrar(10L, concluido(OrigemDoRegistro.FILA), AUTOR);

        verify(acompanhamentos).buscarParaAlteracao(10L);
        verify(acompanhamentos, never()).findById(10L);
    }

    @Test
    @DisplayName("ATD-01: contrato cancelado recusa com \"Contrato cancelado. O atendimento não será gravado.\"")
    void canceladoRecusa() {
        contratoNaSituacao(SituacaoNaCarteira.CANCELADO);

        assertThatThrownBy(() -> servico.registrar(10L, concluido(OrigemDoRegistro.FICHA), AUTOR))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Contrato cancelado. O atendimento não será gravado.");
        verify(atendimentos, never()).save(any());
    }

    @Test
    @DisplayName("FIL-02: pela fila, contrato que saiu dela (suspenso) recebe \"A fila mudou\"")
    void suspensoPelaFila() {
        contratoNaSituacao(SituacaoNaCarteira.SUSPENSO);

        assertThatThrownBy(() -> servico.registrar(10L, concluido(OrigemDoRegistro.FILA), AUTOR))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("fila-mudou");
    }

    @Test
    @DisplayName("ATD-03: se outra pessoa já registrou a mensagem do ano, a nova é recusada e nada é gravado")
    void segundaMensagemDeAniversario() {
        var aniversariante = ContratoNaFilaDeTeste.aniversariante(10L, 7L, "HAA7216", LocalDate.of(1992, 3, 25));
        when(carteira.contrato(10L, Optional.empty())).thenReturn(aniversariante);
        when(tipos.id(TipoDeRelacionamento.ANIVERSARIO)).thenReturn((short) 2);
        when(atendimentos.existsByAssociadoIdAndAnoAniversario(7L, 2026)).thenReturn(true);

        assertThatThrownBy(() -> servico.aniversario(10L, AUTOR))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("aniversario-ja-registrado");
        verify(atendimentos, never()).save(any());
    }
}
