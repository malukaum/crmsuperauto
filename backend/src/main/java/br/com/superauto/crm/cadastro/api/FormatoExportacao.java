package br.com.superauto.crm.cadastro.api;

import java.util.List;
import java.util.function.Function;

import br.com.superauto.crm.cadastro.servico.LinhaExportada;

import org.springframework.http.MediaType;

/** CAR-07 · Formatos do arquivo exportado da carteira. Formato novo entra como mais um valor aqui. */
public enum FormatoExportacao {

    XLSX(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "xlsx",
            ExportacaoExcel::gerar),
    CSV(MediaType.parseMediaType("text/csv;charset=UTF-8"), "csv", ExportacaoCsv::gerar);

    private final MediaType tipo;
    private final String extensao;
    private final Function<List<LinhaExportada>, byte[]> gerador;

    FormatoExportacao(MediaType tipo, String extensao, Function<List<LinhaExportada>, byte[]> gerador) {
        this.tipo = tipo;
        this.extensao = extensao;
        this.gerador = gerador;
    }

    MediaType tipo() {
        return tipo;
    }

    String extensao() {
        return extensao;
    }

    byte[] gerar(List<LinhaExportada> linhas) {
        return gerador.apply(linhas);
    }
}
