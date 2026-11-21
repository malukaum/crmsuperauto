package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFilaDeTeste;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class AtendimentoAvulsoServiceTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO =
            Clock.fixed(LocalDateTime.of(2026, 10, 31, 12, 0).atZone(SAO_PAULO).toInstant(), SAO_PAULO);
    private static final AutorDoContato AUTOR = new AutorDoContato(UUID.randomUUID(), Optional.empty());

    private final CarteiraVisivel carteira = mock(CarteiraVisivel.class);
    private final AtendimentoRepository atendimentos = mock(AtendimentoRepository.class);
    private final TiposDeContato tipos = mock(TiposDeContato.class);
    private final AtendimentoAvulsoService servico = new AtendimentoAvulsoService(carteira, atendimentos, tipos, RELOGIO);

    @BeforeEach
    void preparar() {
        when(carteira.contratosDoAssociado(7L, Optional.empty())).thenReturn(List.of(
                ContratoNaFilaDeTeste.contrato(10L, 7L, SituacaoNaCarteira.CANCELADO, UUID.randomUUID())));
        when(tipos.id(any())).thenReturn((short) 3);
    }

    private static RegistroAvulso avulso(TipoDeRelacionamento tipo, Optional<Long> contrato) {
        return new RegistroAvulso(7L, contrato, tipo, ResultadoContato.CONCLUIDO, Optional.empty(), "Dúvida",
                Optional.empty());
    }

    @Test
    @DisplayName("ATD-02: Reaproximação pode ser registrada para contrato cancelado e fica ligada a ele")
    void reaproximacaoDeCancelado() {
        servico.registrar(avulso(TipoDeRelacionamento.REAPROXIMACAO, Optional.of(10L)), AUTOR);

        var salvo = ArgumentCaptor.forClass(Atendimento.class);
        verify(atendimentos).save(salvo.capture());
        assertThat(salvo.getValue().getContratoId()).contains(10L);
    }

    @Test
    @DisplayName("ATD-02: o contrato é opcional, o contato pode ser sobre a pessoa")
    void semContrato() {
        servico.registrar(avulso(TipoDeRelacionamento.SUPORTE, Optional.empty()), AUTOR);

        var salvo = ArgumentCaptor.forClass(Atendimento.class);
        verify(atendimentos).save(salvo.capture());
        assertThat(salvo.getValue().getContratoId()).isEmpty();
    }

    @Test
    @DisplayName("ATD-02: contrato de outro cliente e tipo que não é avulso são recusados")
    void recusas() {
        assertThatThrownBy(() -> servico.registrar(avulso(TipoDeRelacionamento.SUPORTE, Optional.of(99L)), AUTOR))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Contrato não encontrado.");
        assertThatThrownBy(() -> servico.registrar(avulso(TipoDeRelacionamento.POS_VENDA, Optional.empty()), AUTOR))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("tipo-nao-avulso");
        verify(atendimentos, never()).save(any());
    }
}
