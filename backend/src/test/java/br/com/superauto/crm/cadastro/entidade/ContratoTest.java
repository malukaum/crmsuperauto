package br.com.superauto.crm.cadastro.entidade;

import static br.com.superauto.crm.Identificadores.comId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContratoTest {

    private static final OffsetDateTime ADESAO = OffsetDateTime.parse("2026-10-01T00:00:00-03:00");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 31);
    private static final UUID VENDEDOR = UUID.randomUUID();
    private static final UUID GERENTE = UUID.randomUUID();

    @Test
    @DisplayName("CAR-02: todo contrato nasce \"A conferir\", manual, vigente e com o vendedor (RG-12)")
    void cadastrar() {
        var contrato = contratoAConferir();

        assertThat(contrato.getSituacao()).isEqualTo(SituacaoContrato.A_CONFERIR);
        assertThat(contrato.getOrigem()).isEqualTo(OrigemContrato.MANUAL);
        assertThat(contrato.getVendedorId()).isEqualTo(VENDEDOR);
        assertThat(contrato.vigente()).isTrue();
    }

    @Test
    @DisplayName("CAR-02: a data de adesão não pode ser futura (proposta)")
    void adesaoFutura() {
        var amanha = HOJE.plusDays(1).atStartOfDay().atOffset(ADESAO.getOffset());
        var novo = new NovoContrato(new Adesao(1L, 2L, Mensalidade.de("99.90"), amanha), VENDEDOR,
                Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> Contrato.cadastrar(novo, HOJE, VENDEDOR))
                .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo("adesao-futura"));
    }

    @Test
    @DisplayName("Transferir troca o vendedor e registra o evento com o anterior, o novo e o autor")
    void transferir() {
        var contrato = contratoAConferir();
        UUID outro = UUID.randomUUID();

        contrato.transferirPara(outro, GERENTE);

        assertThat(contrato.getVendedorId()).isEqualTo(outro);
        assertThat(contrato.eventosPendentes()).last().isEqualTo(new ContratoTransferido(null, VENDEDOR, outro, GERENTE));
    }

    @Test
    @DisplayName("Contrato cancelado não muda de vendedor e fica com o original (RG-07)")
    void canceladoNaoTransfere() {
        var contrato = contratoAConferir();
        contrato.cancelar(ADESAO.plusMonths(1), GERENTE);

        assertThatThrownBy(() -> contrato.transferirPara(UUID.randomUUID(), GERENTE))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("contrato-cancelado"));
        assertThat(contrato.getVendedorId()).isEqualTo(VENDEDOR);
    }

    @Test
    @DisplayName("CAR-05: cancelar grava a data de encerramento e gera o evento de situação")
    void cancelar() {
        var contrato = comId(contratoAConferir(), 10L);

        contrato.alterarSituacao(SituacaoContrato.CANCELADO, ADESAO.plusMonths(1), GERENTE);

        assertThat(contrato.getEncerradoEm()).isEqualTo(ADESAO.plusMonths(1));
        assertThat(contrato.eventosPendentes()).containsExactly(new SituacaoContratoAlterada(10L,
                SituacaoContrato.A_CONFERIR, SituacaoContrato.CANCELADO, GERENTE));
    }

    @Test
    @DisplayName("CAR-05: cancelado é definitivo; para voltar, cadastra-se um novo contrato (proposta)")
    void canceladoDefinitivo() {
        var contrato = contratoAConferir();
        contrato.cancelar(ADESAO.plusMonths(1), GERENTE);

        assertThatThrownBy(() -> contrato.alterarSituacao(SituacaoContrato.ATIVO, ADESAO.plusMonths(2), GERENTE))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("transicao-situacao-invalida"));
    }

    @Test
    @DisplayName("CAR-05: reajuste de R$ 124,82 para R$ 139,90 guarda só o atual e leva os dois valores no evento")
    void reajuste() {
        var contrato = comId(contratoCom(Mensalidade.de("124.82")), 10L);

        contrato.reajustarMensalidade(Mensalidade.de("139.90"), GERENTE);

        assertThat(contrato.getMensalidade()).isEqualTo(Mensalidade.de("139.90"));
        assertThat(contrato.eventosPendentes()).containsExactly(
                new MensalidadeReajustada(10L, Mensalidade.de("124.82"), Mensalidade.de("139.90"), GERENTE));
    }

    @Test
    @DisplayName("Alterar para os mesmos valores não gera evento")
    void semMudanca() {
        var contrato = contratoAConferir();

        contrato.reajustarMensalidade(Mensalidade.de("99.90"), GERENTE);
        contrato.transferirPara(VENDEDOR, GERENTE);
        contrato.alterarCodigoSga(Optional.of("  "), GERENTE);
        contrato.alterarSituacao(SituacaoContrato.A_CONFERIR, ADESAO, GERENTE);

        assertThat(contrato.eventosPendentes()).isEmpty();
    }

    @Test
    @DisplayName("RG-06: salvar sobre uma versão diferente da vista é recusado")
    void edicaoSimultanea() {
        assertThatThrownBy(() -> contratoAConferir().conferirVersao(1))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.CONFLITO));
    }

    static Contrato contratoAConferir() {
        return contratoCom(Mensalidade.de("99.90"));
    }

    private static Contrato contratoCom(Mensalidade mensalidade) {
        var novo = new NovoContrato(new Adesao(1L, 2L, mensalidade, ADESAO), VENDEDOR, Optional.empty(), Optional.empty());
        return Contrato.cadastrar(novo, HOJE, VENDEDOR);
    }
}
