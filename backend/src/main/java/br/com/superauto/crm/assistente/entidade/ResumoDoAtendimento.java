package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * IA-02 · O resumo estruturado de um atendimento, no mesmo formato qualquer que seja o provedor de IA. Um resumo
 * fora das regras é recusado, e o assistente tenta de novo (no mesmo provedor ou no reserva).
 */
public record ResumoDoAtendimento(
        String resumoCurto,
        Set<Assunto> assuntos,
        Satisfacao satisfacao,
        List<String> objecoes,
        List<String> interesses,
        boolean riscoDeCancelamento,
        ChanceDeIndicar chanceDeIndicar,
        String proximaAcao) {

    static final int TAMANHO_DO_RESUMO = 500;
    static final int MAXIMO_DE_ITENS = 5;
    static final int TAMANHO_DO_ITEM = 200;

    public ResumoDoAtendimento {
        if (resumoCurto == null || resumoCurto.isBlank() || resumoCurto.length() > TAMANHO_DO_RESUMO)
            throw invalido("O resumo precisa ter de 1 a 500 caracteres.");
        if (assuntos == null || assuntos.isEmpty()) throw invalido("O resumo precisa de ao menos um assunto.");
        Objects.requireNonNull(satisfacao, "satisfacao");
        Objects.requireNonNull(chanceDeIndicar, "chanceDeIndicar");
        assuntos = Set.copyOf(assuntos);
        objecoes = itens(objecoes, "objeções");
        interesses = itens(interesses, "interesses");
        if (proximaAcao != null && proximaAcao.length() > TAMANHO_DO_RESUMO)
            throw invalido("A próxima ação deve ter até 500 caracteres.");
    }

    private static List<String> itens(List<String> itens, String nome) {
        List<String> copia = itens == null ? List.of() : itens.stream().filter(i -> i != null && !i.isBlank()).toList();
        if (copia.size() > MAXIMO_DE_ITENS || copia.stream().anyMatch(i -> i.length() > TAMANHO_DO_ITEM))
            throw invalido("O resumo aceita até 5 " + nome + " de até 200 caracteres.");
        return copia;
    }

    private static NegocioException invalido(String mensagem) {
        return NegocioException.invalido("resumo-invalido", mensagem);
    }
}
