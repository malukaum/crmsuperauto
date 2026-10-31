package br.com.superauto.crm.cadastro.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

import br.com.superauto.crm.cadastro.servico.LinhaExportada;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Exportação da carteira (CAR-07) em Excel (.xlsx). Mensalidade sai como número em reais e as datas como datas,
 * para a planilha somar e ordenar; o cabeçalho fica congelado e com filtro. Texto entra sempre como texto, nunca
 * como fórmula.
 */
final class ExportacaoExcel {

    private static final String ABA = "Carteira";
    private static final String FORMATO_REAIS = "\"R$\" #,##0.00";
    private static final String FORMATO_DATA = "dd/mm/yyyy";
    /** Linhas mantidas em memória pelo modo streaming; as demais vão para disco temporário. */
    private static final int LINHAS_EM_MEMORIA = 200;
    private static final int LARGURA_MINIMA = 10;
    private static final int LARGURA_MAXIMA = 45;
    private static final int UNIDADES_POR_CARACTERE = 256;

    private ExportacaoExcel() {
    }

    static byte[] gerar(List<LinhaExportada> linhas) {
        var planilha = new SXSSFWorkbook(LINHAS_EM_MEMORIA);
        try (planilha; var saida = new ByteArrayOutputStream()) {
            new Escrita(planilha).escrever(linhas);
            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a planilha da carteira.", e);
        }
    }

    /** Uma escrita da planilha, com os estilos criados uma vez só (o Excel limita a quantidade de estilos). */
    private static final class Escrita {

        private final Sheet aba;
        private final CellStyle cabecalho;
        private final CellStyle reais;
        private final CellStyle data;

        Escrita(Workbook planilha) {
            this.aba = planilha.createSheet(ABA);
            this.cabecalho = estiloCabecalho(planilha);
            this.reais = estiloNumerico(planilha, FORMATO_REAIS);
            this.data = estiloNumerico(planilha, FORMATO_DATA);
        }

        void escrever(List<LinhaExportada> linhas) {
            escreverCabecalho();
            int numero = 1;
            for (LinhaExportada linha : linhas) {
                escreverLinha(aba.createRow(numero++), ColunasExportacao.valores(linha));
            }
            int ultimaColuna = ColunasExportacao.CABECALHO.size() - 1;
            aba.createFreezePane(0, 1);
            aba.setAutoFilter(new CellRangeAddress(0, Math.max(linhas.size(), 1), 0, ultimaColuna));
        }

        private void escreverCabecalho() {
            Row linha = aba.createRow(0);
            List<String> titulos = ColunasExportacao.CABECALHO;
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
                case ColunasExportacao.Reais r -> {
                    celula.setCellValue(r.valor().doubleValue());
                    celula.setCellStyle(reais);
                }
                case LocalDate d -> {
                    celula.setCellValue(d);
                    celula.setCellStyle(data);
                }
                case Integer n -> celula.setCellValue(n);
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
