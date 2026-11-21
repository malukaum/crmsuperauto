package br.com.superauto.crm.assistente.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class AnotacaoDoAtendimentoTest {
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-11-21T10:00:00-03:00");
    private static final NovoAnexo PDF = new NovoAnexo("orcamento.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8));

    private static AnotacaoDoAtendimento anotacao() {
        return AnotacaoDoAtendimento.registrar(new NovaAnotacao(10L, 7L, "Cliente reclamou do guincho", List.of(PDF)),
                UUID.randomUUID());
    }

    private static ResumoDoAtendimento resumo() {
        return new ResumoDoAtendimento("Reclamou da demora do guincho.", Set.of(Assunto.GUINCHO), Satisfacao.NEGATIVA,
                List.of("Demora do guincho"), List.of(), true, ChanceDeIndicar.BAIXA, "Ligar em 2 dias");
    }

    @Test
    @DisplayName("IA-01: anotação sem texto nem anexo, com mais de 5 anexos ou com tipo não aceito é recusada")
    void validacoes() {
        assertThatThrownBy(() -> new NovaAnotacao(10L, 7L, " ", List.of())).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> new NovaAnotacao(10L, 7L, "ok", Collections.nCopies(6, PDF)))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> new NovoAnexo("planilha.xlsx", "application/vnd.ms-excel", new byte[] {1}))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    @DisplayName("IA-01: a anotação nasce aguardando o resumo, com o anexo guardado até lá")
    void nasceAguardando() {
        var anotacao = anotacao();

        assertThat(anotacao.getSituacao()).isEqualTo(SituacaoDoResumo.AGUARDANDO);
        assertThat(anotacao.getAnexos()).singleElement().satisfies(a -> assertThat(a.getConteudo()).isPresent());
    }

    @Test
    @DisplayName("IA-02: com o resumo pronto, o conteúdo dos anexos é apagado; ficam nome, tipo, tamanho e hash")
    void resumoDescartaAnexos() {
        var anotacao = anotacao();

        anotacao.registrarResumo(resumo(), ProvedorDeIa.CLAUDE, "claude-opus-5-5", AGORA);

        assertThat(anotacao.getSituacao()).isEqualTo(SituacaoDoResumo.RESUMIDO);
        assertThat(anotacao.resumo()).hasValueSatisfying(r -> assertThat(r.assuntos()).containsExactly(Assunto.GUINCHO));
        assertThat(anotacao.getAnexos()).singleElement().satisfies(a -> {
            assertThat(a.getConteudo()).isEmpty();
            assertThat(a.getHash()).hasSize(64);
        });
    }

    @Test
    @DisplayName("IA-01: cada falha adia a nova tentativa; na quinta, desiste e descarta os anexos")
    void falhas() {
        var anotacao = anotacao();

        anotacao.registrarFalha("fora do ar", AGORA);
        assertThat(anotacao.getSituacao()).isEqualTo(SituacaoDoResumo.AGUARDANDO);
        for (int i = 0; i < 4; i++) anotacao.registrarFalha("fora do ar", AGORA);

        assertThat(anotacao.getSituacao()).isEqualTo(SituacaoDoResumo.FALHOU);
        assertThat(anotacao.getAnexos()).allSatisfy(a -> assertThat(a.getConteudo()).isEmpty());
    }

    @Test
    @DisplayName("IA-06: anonimizar apaga texto e resumo escrito, mas mantém os sinais que entram nos indicadores")
    void anonimizar() {
        var anotacao = anotacao();
        anotacao.registrarResumo(resumo(), ProvedorDeIa.CLAUDE, "m", AGORA);

        anotacao.anonimizar(AGORA);

        assertThat(anotacao.getTexto()).isEmpty();
        assertThat(anotacao.resumo()).isEmpty();
        assertThat(anotacao.getAnexos()).isEmpty();
    }
}
