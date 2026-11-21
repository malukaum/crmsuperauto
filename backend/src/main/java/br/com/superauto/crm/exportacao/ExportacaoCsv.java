package br.com.superauto.crm.exportacao;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exportação em CSV com os formatos da RG-02. Usa ";" e o BOM do UTF-8 para o Excel em português abrir as colunas e
 * os acentos certos sem importação manual.
 */
final class ExportacaoCsv {

    private static final String SEPARADOR = ";";
    private static final String FIM_DE_LINHA = "\r\n";
    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final Locale PORTUGUES_DO_BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_E_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ExportacaoCsv() {
    }

    static byte[] gerar(Planilha planilha) {
        Stream<List<?>> cabecalho = Stream.of(planilha.cabecalho());
        String conteudo = Stream.concat(cabecalho, planilha.linhas().stream())
                .map(ExportacaoCsv::linha)
                .collect(Collectors.joining(FIM_DE_LINHA, "", FIM_DE_LINHA));
        var saida = new ByteArrayOutputStream();
        saida.writeBytes(BOM_UTF8);
        saida.writeBytes(conteudo.getBytes(StandardCharsets.UTF_8));
        return saida.toByteArray();
    }

    private static String linha(List<?> valores) {
        return valores.stream().map(ExportacaoCsv::texto).map(ExportacaoCsv::celula).collect(Collectors.joining(SEPARADOR));
    }

    private static String texto(Object valor) {
        return switch (valor) {
            case null -> "";
            case ValorEmReais reais -> NumberFormat.getCurrencyInstance(PORTUGUES_DO_BRASIL).format(reais.valor());
            case LocalDate data -> DATA.format(data);
            case LocalDateTime dataEHora -> DATA_E_HORA.format(dataEHora);
            default -> String.valueOf(valor);
        };
    }

    /** Aspas em toda célula; aspas internas dobradas. Fórmulas não são interpretadas pelo Excel ("=", "+"...). */
    private static String celula(String valor) {
        String seguro = valor.matches("^[=+\\-@].*") ? "'" + valor : valor;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }
}
