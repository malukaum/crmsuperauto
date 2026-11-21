package br.com.superauto.crm.exportacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Conteúdo de um arquivo exportado (CAR-07, ATD-05), igual em todos os formatos. Cada valor sai tipado para o
 * formato aplicar a RG-02 do seu jeito: {@code String}, {@link ValorEmReais}, {@code LocalDate},
 * {@code LocalDateTime}, {@code Integer}, {@code Long} ou {@code null} (célula vazia).
 *
 * @param aba nome da aba no Excel
 */
public record Planilha(String aba, List<String> cabecalho, List<List<Object>> linhas) {

    public Planilha {
        Objects.requireNonNull(aba, "aba");
        cabecalho = List.copyOf(cabecalho);
        List<List<Object>> copia = new ArrayList<>();
        for (List<Object> linha : linhas) {
            if (linha.size() != cabecalho.size()) {
                throw new IllegalArgumentException("Cada linha precisa ter um valor por coluna do cabeçalho.");
            }
            copia.add(Collections.unmodifiableList(new ArrayList<>(linha)));
        }
        linhas = Collections.unmodifiableList(copia);
    }
}
