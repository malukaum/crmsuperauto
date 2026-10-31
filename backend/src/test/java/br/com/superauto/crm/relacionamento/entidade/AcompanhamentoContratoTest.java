package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.erro.NegocioException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

class AcompanhamentoContratoTest {

    static final EtapaRegua D7 = new EtapaRegua((short) 1, "D7", "D+7", 7, (short) 2, true);
    static final EtapaRegua D30 = new EtapaRegua((short) 2, "D30", "D+30", 23, (short) 3, false);
    static final EtapaRegua D60 = new EtapaRegua((short) 3, "D60", "D+60", 30, (short) 4, false);
    static final EtapaRegua D90 = new EtapaRegua((short) 4, "D90", "D+90", 30, (short) 5, false);
    static final EtapaRegua TRIMESTRAL =
            new EtapaRegua((short) 5, "TRIMESTRAL", "Trimestral", 90, (short) 5, false);
    static final List<EtapaRegua> REGUA = List.of(D7, D30, D60, D90, TRIMESTRAL);

    @Test
    @DisplayName(
            "CAR-02: dada a adesão em 13/03/2026, o próximo contato é 20/03/2026 e a etapa pendente"
                    + " é D+7")
    void criterioDeAceite() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));

        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 3, 20));
        assertThat(acompanhamento.getEtapaPendenteId()).contains(D7.id());
        assertThat(acompanhamento.getId()).isEqualTo(10L);
        assertThat(acompanhamento.isNew()).isTrue();
    }

    @Test
    @DisplayName("A etapa inicial é a marcada como inicial, qualquer que seja a posição na lista")
    void usaAEtapaInicial() {
        var acompanhamento =
                AcompanhamentoContrato.iniciar(10L, List.of(D30, D7), LocalDate.of(2026, 3, 13));

        assertThat(acompanhamento.getEtapaPendenteId()).contains(D7.id());
    }

    private static AcompanhamentoContrato.ContextoContato em(LocalDate hoje, int versao) {
        return new AcompanhamentoContrato.ContextoContato(
                versao, hoje, hoje.atStartOfDay().atOffset(ZoneOffset.ofHours(-3)), REGUA);
    }

    @Test
    @DisplayName("FIL-02: concluir D+7 em 25/03 avança para D+30 em 17/04")
    void concluirAvancaEtapa() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        acompanhamento.registrar(
                ResultadoContato.CONCLUIDO, Optional.empty(), em(LocalDate.of(2026, 3, 25), 0));
        assertThat(acompanhamento.getEtapaPendenteId()).contains(D30.id());
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 4, 17));
    }

    @Test
    @DisplayName("FIL-03: sem resposta em 31/10 agenda nova tentativa em 02/11 sem mudar a etapa")
    void semResposta() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        acompanhamento.registrar(
                ResultadoContato.SEM_RESPOSTA, Optional.empty(), em(LocalDate.of(2026, 10, 31), 0));
        assertThat(acompanhamento.getEtapaPendenteId()).contains(D7.id());
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 11, 2));
    }

    @Test
    @DisplayName("FIL-06: alterar D+30 de 23 para 25 dias leva o retorno de 17/04 para 19/04")
    void recalcularPrazos() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        var concluido = LocalDate.of(2026, 3, 25);
        acompanhamento.registrar(ResultadoContato.CONCLUIDO, Optional.empty(), em(concluido, 0));
        var novaRegua =
                REGUA.stream()
                        .map(
                                e ->
                                        e.id().equals(D30.id())
                                                ? new EtapaRegua(
                                                        e.id(),
                                                        e.codigo(),
                                                        e.nome(),
                                                        25,
                                                        e.proximaEtapaId(),
                                                        e.inicial())
                                                : e)
                        .toList();
        acompanhamento.recalcular(concluido, novaRegua);
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 4, 19));
    }

    @Test
    @DisplayName("FIL-06: a mudança dos prazos preserva retorno manual em 05/11")
    void preservaManual() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        var manual = LocalDate.of(2026, 11, 5);
        acompanhamento.registrar(
                ResultadoContato.REAGENDADO,
                Optional.of(manual),
                em(LocalDate.of(2026, 10, 31), 0));
        acompanhamento.recalcular(
                LocalDate.of(2026, 3, 13),
                List.of(new EtapaRegua(D7.id(), "D7", "D+7", 365, D30.id(), true)));
        assertThat(acompanhamento.getProximoContatoEm()).contains(manual);
    }

    @Test
    @DisplayName("FIL-02 / RG-06: a versão desatualizada não sobrescreve a régua")
    void recusaVersaoAntiga() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        Assertions.assertThatThrownBy(
                        () ->
                                acompanhamento.registrar(
                                        ResultadoContato.CONCLUIDO,
                                        Optional.empty(),
                                        em(LocalDate.of(2026, 3, 25), 1)))
                .isInstanceOf(NegocioException.class);
        assertThat(acompanhamento.getEtapaPendenteId()).contains(D7.id());
    }

    @Test
    @DisplayName("Reagendado exige uma data e nenhum retorno aceita ontem")
    void validaRetorno() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        var hoje = LocalDate.of(2026, 10, 31);
        Assertions.assertThatThrownBy(
                        () ->
                                acompanhamento.registrar(
                                        ResultadoContato.REAGENDADO, Optional.empty(), em(hoje, 0)))
                .isInstanceOf(NegocioException.class);
        Assertions.assertThatThrownBy(
                        () ->
                                acompanhamento.registrar(
                                        ResultadoContato.CONCLUIDO,
                                        Optional.of(hoje.minusDays(1)),
                                        em(hoje, 0)))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    @DisplayName("A prévia mostra a nova etapa e data sem alterar o acompanhamento")
    void previaNaoAlteraRegua() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        var previa =
                acompanhamento.prever(
                        ResultadoContato.CONCLUIDO,
                        Optional.empty(),
                        em(LocalDate.of(2026, 3, 25), 0));
        assertThat(previa.getProximoContatoEm()).contains(LocalDate.of(2026, 4, 17));
        assertThat(acompanhamento.getEtapaPendenteId()).contains(D7.id());
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 3, 20));
    }

    @Test
    @DisplayName("A etapa trimestral repete com prazo de 90 dias; Pendente mantém a data")
    void trimestralEPendente() {
        var acompanhamento = AcompanhamentoContrato.iniciar(10L, REGUA, LocalDate.of(2026, 3, 13));
        for (int i = 0; i < 4; i++)
            acompanhamento.registrar(
                    ResultadoContato.CONCLUIDO, Optional.empty(), em(LocalDate.of(2026, 6, 1), 0));
        acompanhamento.registrar(
                ResultadoContato.CONCLUIDO, Optional.empty(), em(LocalDate.of(2026, 6, 1), 0));
        assertThat(acompanhamento.getEtapaPendenteId()).contains(TRIMESTRAL.id());
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 8, 30));
        acompanhamento.registrar(
                ResultadoContato.PENDENTE, Optional.empty(), em(LocalDate.of(2026, 6, 2), 0));
        assertThat(acompanhamento.getProximoContatoEm()).contains(LocalDate.of(2026, 8, 30));
    }
}
