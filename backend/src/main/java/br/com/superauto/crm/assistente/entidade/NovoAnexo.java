package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.util.Objects;
import java.util.Set;

/**
 * IA-01 · Um arquivo anexado à anotação, como chegou da tela. Aceita PDF, imagens (JPG, PNG) e texto, até 10 MB.
 */
public record NovoAnexo(String nome, String tipoConteudo, byte[] conteudo) {
    static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;
    static final Set<String> TIPOS_ACEITOS = Set.of("application/pdf", "image/jpeg", "image/png", "text/plain");

    public NovoAnexo {
        Objects.requireNonNull(conteudo, "conteudo");
        if (nome == null || nome.isBlank()) throw invalido("O anexo precisa de um nome.");
        if (!TIPOS_ACEITOS.contains(tipoConteudo))
            throw invalido("Anexe só PDF, imagem (JPG ou PNG) ou texto: " + nome + ".");
        if (conteudo.length == 0 || conteudo.length > TAMANHO_MAXIMO)
            throw invalido("Cada anexo pode ter até 10 MB: " + nome + ".");
        nome = nome.length() > 255 ? nome.substring(0, 255) : nome;
    }

    private static NegocioException invalido(String mensagem) {
        return NegocioException.invalido("anexo-invalido", mensagem);
    }
}
