package br.com.superauto.crm.cadastro.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.servico.LinhaExportada;
import br.com.superauto.crm.compartilhado.entidade.Fila;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExportacaoExcelTest {

    private static final int MENSALIDADE = 9;
    private static final int PROXIMO_CONTATO = 12;

    @Test
    @DisplayName("CAR-07: cabeçalho com o aviso da RG-09, congelado e com filtro")
    void cabecalho() throws IOException {
        Sheet aba = ler(List.of(linha("José da Silva")));

        assertThat(aba.getSheetName()).isEqualTo("Carteira");
        assertThat(aba.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Cliente");
        assertThat(aba.getRow(0).getCell(MENSALIDADE).getStringCellValue()).isEqualTo("Mensalidade contratada (não é receita)");
        assertThat(aba.getPaneInformation().isFreezePane()).isTrue();
    }

    @Test
    @DisplayName("CAR-07: mensalidade sai como número em reais e o próximo contato como data dd/mm/aaaa")
    void valoresTipados() throws IOException {
        Row linha = ler(List.of(linha("José da Silva"))).getRow(1);

        assertThat(linha.getCell(MENSALIDADE).getNumericCellValue()).isEqualTo(1234.56);
        assertThat(linha.getCell(MENSALIDADE).getCellStyle().getDataFormatString()).isEqualTo("\"R$\" #,##0.00");
        assertThat(linha.getCell(PROXIMO_CONTATO).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(linha.getCell(PROXIMO_CONTATO).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
    }

    @Test
    @DisplayName("CAR-07: documento como veio do serviço e contato com opt-out marcado")
    void documentoEOptOut() throws IOException {
        Row linha = ler(List.of(linha("José da Silva"))).getRow(1);

        assertThat(linha.getCell(1).getStringCellValue()).isEqualTo("***.***.247-**");
        assertThat(linha.getCell(3).getStringCellValue()).isEqualTo("Não");
        assertThat(linha.getCell(10).getStringCellValue()).isEqualTo("A conferir");
    }

    @Test
    @DisplayName("Texto parecido com fórmula entra como texto, nunca como fórmula")
    void semFormulas() throws IOException {
        Row linha = ler(List.of(linha("=HYPERLINK(\"x\")"))).getRow(1);

        assertThat(linha.getCell(0).getCellType()).isEqualTo(CellType.STRING);
        assertThat(linha.getCell(0).getStringCellValue()).isEqualTo("=HYPERLINK(\"x\")");
    }

    @Test
    @DisplayName("Lista vazia gera só o cabeçalho")
    void vazia() throws IOException {
        assertThat(ler(List.of()).getLastRowNum()).isZero();
    }

    private static Sheet ler(List<LinhaExportada> linhas) throws IOException {
        try (var planilha = new XSSFWorkbook(new ByteArrayInputStream(ExportacaoExcel.gerar(linhas)))) {
            return planilha.getSheetAt(0);
        }
    }

    private static LinhaExportada linha(String associado) {
        return new LinhaExportada(associado, "***.***.247-**", "(37) 9 9869-2950", true, "HAA7216", "Onix", "Carro",
                "Formiga", "MG", new BigDecimal("1234.56"), SituacaoContrato.A_CONFERIR, "D+7", LocalDate.of(2026, 3, 20),
                Fila.ATRASADO, 225, "Vendedor Um");
    }
}
