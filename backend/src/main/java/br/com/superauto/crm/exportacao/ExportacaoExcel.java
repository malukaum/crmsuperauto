package br.com.superauto.crm.exportacao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Exportação em Excel (.xlsx). Valores em reais saem como número e as datas como datas, para a planilha somar e
 * ordenar; o cabeçalho fica congelado e com filtro. Texto entra sempre como texto, nunca
 * como fórmula.
 */
final class ExportacaoExcel {

    private static final String FORMATO_REAIS = "\"R$\" #,##0.00";
    private static final String FORMATO_DATA = "dd/mm/yyyy";
    private static final String FORMATO_DATA_E_HORA = "dd/mm/yyyy hh:mm";
    /** Linhas mantidas em memória pelo modo streaming; as demais vão para disco temporário. */
    private static final int LINHAS_EM_MEMORIA = 200;
    private static final int LARGURA_MINIMA = 10;
    private static final int LARGURA_MAXIMA = 45;
    private static final int UNIDADES_POR_CARACTERE = 256;

    private ExportacaoExcel() {
    }

    static byte[] gerar(Planilha conteudo) {
        var planilha = new SXSSFWorkbook(LINHAS_EM_MEMORIA);
        try (planilha; var saida = new ByteArrayOutputStream()) {
            new Escrita(planilha, conteudo.aba()).escrever(conteudo);
            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a planilha.", e);
        }
    }

    /** Uma escrita da planilha, com os estilos criados uma vez só (o Excel limita a quantidade de estilos). */
    private static final class Escrita {

        private final Sheet aba;
        private final CellStyle cabecalho;
        private final CellStyle reais;
        private final CellStyle data;
        private final CellStyle dataEHora;

        Escrita(Workbook planilha, String nomeDaAba) {
            this.aba = planilha.createSheet(nomeDaAba);
            this.cabecalho = estiloCabecalho(planilha);
            this.reais = estiloNumerico(planilha, FORMATO_REAIS);
            this.data = estiloNumerico(planilha, FORMATO_DATA);
            this.dataEHora = estiloNumerico(planilha, FORMATO_DATA_E_HORA);
        }

        void escrever(Planilha conteudo) {
            escreverCabecalho(conteudo.cabecalho());
            int numero = 1;
            for (List<Object> linha : conteudo.linhas()) {
                escreverLinha(aba.createRow(numero++), linha);
            }
            int ultimaColuna = conteudo.cabecalho().size() - 1;
            aba.createFreezePane(0, 1);
            aba.setAutoFilter(new CellRangeAddress(0, Math.max(conteudo.linhas().size(), 1), 0, ultimaColuna));
        }

        private void escreverCabecalho(List<String> titulos) {
            Row linha = aba.createRow(0);
            for (int coluna = 0; coluna < titulos.size(); coluna++) {
                Cell celula = linha.createCell(coluna);
                celula.setCellValue(titulos.get(coluna));
                celula.setCellStyle(cabecalho);
                int caracteres = Math.clamp(titulos.get(coluna).length() + 2, LARGURA_MINIMA, LARGURA_MAXIMA);
                aba.setColumnWidth(coluna, caracteres * UNIDADES_POR_CARACTERE);
            }
        }

        private void escreverLinha(Row linha, List<Object> valores) {
            for (int coluna = 0; coluna < valores.size(); coluna++) {
                preencher(linha.createCell(coluna), valores.get(coluna));
            }
        }

        private void preencher(Cell celula, Object valor) {
            switch (valor) {
                case null -> celula.setBlank();
                case ValorEmReais r -> {
                    celula.setCellValue(r.valor().doubleValue());
                    celula.setCellStyle(reais);
                }
                case LocalDate d -> {
                    celula.setCellValue(d);
                    celula.setCellStyle(data);
                }
                case LocalDateTime d -> {
                    celula.setCellValue(d);
                    celula.setCellStyle(dataEHora);
                }
                case Integer n -> celula.setCellValue(n);
                case Long n -> celula.setCellValue(n);
                default -> celula.setCellValue(String.valueOf(valor));
            }
        }

        private static CellStyle estiloCabecalho(Workbook planilha) {
            Font negrito = planilha.createFont();
            negrito.setBold(true);
            CellStyle estilo = planilha.createCellStyle();
            estilo.setFont(negrito);
            return estilo;
        }

        private static CellStyle estiloNumerico(Workbook planilha, String formato) {
            CellStyle estilo = planilha.createCellStyle();
            estilo.setDataFormat(planilha.createDataFormat().getFormat(formato));
            return estilo;
        }
    }
}
