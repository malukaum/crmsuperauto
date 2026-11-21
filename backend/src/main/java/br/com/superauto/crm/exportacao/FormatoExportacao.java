package br.com.superauto.crm.exportacao;

import java.util.function.Function;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** CAR-07 · Formatos dos arquivos exportados. Formato novo entra como mais um valor aqui. */
public enum FormatoExportacao {

    XLSX(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "xlsx",
            ExportacaoExcel::gerar),
    CSV(MediaType.parseMediaType("text/csv;charset=UTF-8"), "csv", ExportacaoCsv::gerar);

    private final MediaType tipo;
    private final String extensao;
    private final Function<Planilha, byte[]> gerador;

    FormatoExportacao(MediaType tipo, String extensao, Function<Planilha, byte[]> gerador) {
        this.tipo = tipo;
        this.extensao = extensao;
        this.gerador = gerador;
    }

    public byte[] gerar(Planilha planilha) {
        return gerador.apply(planilha);
    }

    /** A resposta HTTP com o arquivo para baixar, ex.: {@code carteira-2026-10-31.xlsx}. */
    public ResponseEntity<byte[]> anexo(Planilha planilha, String nomeSemExtensao) {
        String nome = nomeSemExtensao + "." + extensao;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .body(gerar(planilha));
    }
}
