package br.com.superauto.crm.relacionamento.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.Identificadores;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

class AtendimentoTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final UUID AUTOR = UUID.randomUUID();
    private static final ZonedDateTime REGISTRO = LocalDateTime.of(2026, 10, 30, 10, 0).atZone(SAO_PAULO);
    private static final Correcao SEM_RESPOSTA =
            new Correcao(ResultadoContato.SEM_RESPOSTA, Optional.empty(), "Não atendeu");

    @Test
    @DisplayName("ATD-01/ATD-02: Reagendado sem data é recusado")
    void reagendadoExigeData() {
        assertThatThrownBy(() -> Atendimento.registrar(novo(ResultadoContato.REAGENDADO, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Informe a data combinada para o retorno.");
    }

    @Test
    @DisplayName("ATD-04: a correção aponta para o original e mantém contrato, etapa e data do contato")
    void correcaoApontaParaOOriginal() {
        var original = registrado();

        var correcao = original.corrigir(SEM_RESPOSTA, new ResponsavelPelaCorrecao(AUTOR, false), REGISTRO.plusHours(2));

        assertThat(correcao.getCorrigeAtendimentoId()).contains(10L);
        assertThat(correcao.getContratoId()).contains(5L);
        assertThat(correcao.getAssociadoId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("ATD-04: o autor corrige até o fim do dia seguinte ao registro")
    void prazoDoAutor() {
        var original = registrado();
        var responsavel = new ResponsavelPelaCorrecao(AUTOR, false);

        assertThat(original.corrigir(SEM_RESPOSTA, responsavel, REGISTRO.plusDays(1).withHour(23).withMinute(59)))
                .isNotNull();
        assertThatThrownBy(() -> original.corrigir(SEM_RESPOSTA, responsavel, REGISTRO.plusDays(2).withHour(0)))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("correcao-fora-do-prazo");
    }

    @Test
    @DisplayName("ATD-04: o gerencial corrige a qualquer momento; outro usuário nunca")
    void quemPodeCorrigir() {
        var original = registrado();

        assertThat(original.corrigir(SEM_RESPOSTA, new ResponsavelPelaCorrecao(UUID.randomUUID(), true), REGISTRO.plusMonths(3)))
                .isNotNull();
        assertThatThrownBy(() -> original.corrigir(SEM_RESPOSTA, new ResponsavelPelaCorrecao(UUID.randomUUID(), false), REGISTRO))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("correcao-nao-permitida");
    }

    @Test
    @DisplayName("ATD-04: atendimento que já tem correção não recebe outra; corrige-se a correção")
    void umaCorrecaoPorAtendimento() {
        var original = registrado();

        assertThatThrownBy(() -> original.exigirSemCorrecao(true))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("atendimento-ja-corrigido");
    }

    @Test
    @DisplayName("ATD-02: só Suporte, Comercial, Reaproximação e Indicação são avulsos")
    void tiposAvulsos() {
        TipoDeRelacionamento.SUPORTE.exigirAvulso();

        assertThatThrownBy(TipoDeRelacionamento.POS_VENDA::exigirAvulso)
                .isInstanceOf(NegocioException.class)
                .extracting("codigo")
                .isEqualTo("tipo-nao-avulso");
    }

    private static Atendimento.Novo novo(ResultadoContato resultado, LocalDate retorno) {
        return new Atendimento.Novo(7L, 5L, (short) 1, (short) 1, resultado, null, retorno,
                REGISTRO.toOffsetDateTime(), AUTOR);
    }

    /** Como o banco devolveria: com id e data de registro. */
    private static Atendimento registrado() {
        var atendimento = Identificadores.comId(Atendimento.registrar(novo(ResultadoContato.CONCLUIDO, null)), 10L);
        definirRegistradoEm(atendimento, REGISTRO.toOffsetDateTime());
        return atendimento;
    }

    private static void definirRegistradoEm(Atendimento atendimento, OffsetDateTime quando) {
        try {
            Field campo = Atendimento.class.getDeclaredField("registradoEm");
            campo.setAccessible(true);
            campo.set(atendimento, quando);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
