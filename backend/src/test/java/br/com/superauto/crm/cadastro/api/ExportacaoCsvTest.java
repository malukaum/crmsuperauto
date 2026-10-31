package br.com.superauto.crm.cadastro.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.servico.LinhaExportada;
import br.com.superauto.crm.compartilhado.entidade.Fila;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExportacaoCsvTest {

    @Test
    @DisplayName("CAR-07: o CSV usa \";\", formatos da RG-02 e marca o opt-out")
    void formatos() {
        var linha = new LinhaExportada("José \"Zé\" Silva", "***.***.247-**", "(37) 9 9869-2950", true, "HAA7216",
                "Onix", "Carro", "Formiga", "MG", new BigDecimal("1234.56"), SituacaoContrato.A_CONFERIR, "D+7",
                LocalDate.of(2026, 3, 20), Fila.ATRASADO, 225, "Vendedor Um");

        String csv = new String(ExportacaoCsv.gerar(List.of(linha)), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿\"Cliente\";");
        assertThat(csv).contains("\"José \"\"Zé\"\" Silva\";\"***.***.247-**\";\"(37) 9 9869-2950\";\"Não\"");
        assertThat(csv).contains("\"R$ 1.234,56\";\"A conferir\";\"D+7\";\"20/03/2026\";\"Atrasado\";\"225\"");
    }

    @Test
    @DisplayName("Texto que o Excel leria como fórmula sai neutralizado")
    void semFormulas() {
        var linha = new LinhaExportada("=HYPERLINK(\"x\")", "", "", false, "HAA7216", "Onix", "Carro", null, null,
                BigDecimal.ONE, SituacaoContrato.ATIVO, "", null, Fila.SEM_DATA, 0, "");

        assertThat(new String(ExportacaoCsv.gerar(List.of(linha)), StandardCharsets.UTF_8)).contains("\"'=HYPERLINK");
    }
}
