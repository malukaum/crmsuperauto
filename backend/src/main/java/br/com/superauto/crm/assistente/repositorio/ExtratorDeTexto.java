package br.com.superauto.crm.assistente.repositorio;

import java.util.Optional;

/**
 * IA-02 · Porta para tirar o texto de um anexo no nosso servidor, antes da pseudonimização. Vazio quando o tipo
 * não tem texto extraível no protótipo (imagens: sem OCR por enquanto, então não são enviadas).
 */
public interface ExtratorDeTexto {

    Optional<String> extrair(String tipoConteudo, byte[] conteudo);
}
