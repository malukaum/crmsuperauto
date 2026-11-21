package br.com.superauto.crm.assistente.infraestrutura;

import br.com.superauto.crm.assistente.repositorio.ExtratorDeTexto;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** IA-02 · Tira o texto de PDF (Apache PDFBox) e de arquivo de texto, no nosso servidor. Imagens: sem OCR ainda. */
@Slf4j
@Component
public class ExtratorDeTextoPdfBox implements ExtratorDeTexto {

    @Override
    public Optional<String> extrair(String tipoConteudo, byte[] conteudo) {
        return switch (tipoConteudo) {
            case "text/plain" -> naoVazio(new String(conteudo, StandardCharsets.UTF_8));
            case "application/pdf" -> doPdf(conteudo);
            default -> Optional.empty();
        };
    }

    private static Optional<String> doPdf(byte[] conteudo) {
        try (var documento = Loader.loadPDF(conteudo)) {
            return naoVazio(new PDFTextStripper().getText(documento));
        } catch (IOException e) {
            log.warn("PDF anexado não pôde ser lido: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> naoVazio(String texto) {
        return Optional.ofNullable(texto).map(String::strip).filter(t -> !t.isEmpty());
    }
}
