package br.com.superauto.crm.cadastro.entidade;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.compartilhado.entidade.Fila;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

class LinhaCarteiraTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 31);
    private static final Short D7 = 1;

    @Test
    @DisplayName("RG-01 / RG-14: a fila da carteira usa o hoje informado pela aplicação, não a data do banco")
    void filaPeloHojeDaAplicacao() {
        var linha = linha(SituacaoContrato.ATIVO, D7, HOJE);

        assertThat(linha.filaEm(HOJE)).isEqualTo(Fila.HOJE);
        assertThat(linha.filaEm(HOJE.plusDays(2))).isEqualTo(Fila.ATRASADO);
        assertThat(linha.diasDeAtrasoEm(HOJE.plusDays(2))).isEqualTo(2);
    }

    @Test
    @DisplayName("CAR-01: suspenso e cancelado ficam na própria fila; sem régua ativa é Sem data")
    void situacaoESemRegua() {
        assertThat(linha(SituacaoContrato.SUSPENSO, D7, HOJE.minusDays(5)).filaEm(HOJE)).isEqualTo(Fila.SUSPENSO);
        assertThat(linha(SituacaoContrato.CANCELADO, null, null).filaEm(HOJE)).isEqualTo(Fila.CANCELADO);
        var semRegua = linha(SituacaoContrato.A_CONFERIR, null, null);
        assertThat(semRegua.filaEm(HOJE)).isEqualTo(Fila.SEM_DATA);
        assertThat(semRegua.diasDeAtrasoEm(HOJE)).isZero();
    }

    /** Como a visão devolveria a linha. */
    private static LinhaCarteira linha(SituacaoContrato situacao, Short etapaPendenteId, LocalDate proximoContato) {
        try {
            var construtor = LinhaCarteira.class.getDeclaredConstructor();
            construtor.setAccessible(true);
            var linha = construtor.newInstance();
            definir(linha, "situacao", situacao);
            definir(linha, "etapaPendenteId", etapaPendenteId);
            definir(linha, "proximoContatoEm", proximoContato);
            return linha;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void definir(LinhaCarteira linha, String campo, Object valor) throws ReflectiveOperationException {
        Field f = LinhaCarteira.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(linha, valor);
    }
}
