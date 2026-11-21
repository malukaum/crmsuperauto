package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

/**
 * IA-04 · Uma sugestão de abordagem e em que ela se baseia. É só sugestão: nunca envia mensagem nem altera nada.
 */
public record Recomendacao(String abordagem, String base) {
    static final int TAMANHO_MAXIMO = 600;

    public Recomendacao {
        if (abordagem == null || abordagem.isBlank() || abordagem.length() > TAMANHO_MAXIMO)
            throw NegocioException.invalido("recomendacao-invalida", "Recomendação sem texto ou longa demais.");
        base = base == null ? "" : base.strip();
    }
}
