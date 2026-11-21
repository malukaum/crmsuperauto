package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.util.List;
import java.util.Objects;

/** IA-01 · O que a equipe anota num atendimento: texto livre (até 10.000 caracteres) e até 5 anexos. */
public record NovaAnotacao(Long atendimentoId, Long associadoId, String texto, List<NovoAnexo> anexos) {
    static final int TAMANHO_DO_TEXTO = 10_000;
    static final int MAXIMO_DE_ANEXOS = 5;

    public NovaAnotacao {
        Objects.requireNonNull(atendimentoId, "atendimentoId");
        Objects.requireNonNull(associadoId, "associadoId");
        texto = texto == null || texto.isBlank() ? null : texto.strip();
        anexos = anexos == null ? List.of() : List.copyOf(anexos);
        if (texto == null && anexos.isEmpty())
            throw NegocioException.invalido("anotacao-vazia", "Escreva uma anotação ou anexe um arquivo.");
        if (texto != null && texto.length() > TAMANHO_DO_TEXTO)
            throw NegocioException.invalido("anotacao-longa", "A anotação pode ter até 10.000 caracteres.");
        if (anexos.size() > MAXIMO_DE_ANEXOS)
            throw NegocioException.invalido("anexos-demais", "Anexe até 5 arquivos por atendimento.");
    }
}
