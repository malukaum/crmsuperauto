package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.Assunto;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * As instruções enviadas aos provedores, iguais para Claude, Gemini e ChatGPT. Pedem resposta num formato fixo, que
 * o sistema valida; o que vier fora dele é recusado e tentado de novo.
 */
final class InstrucoesDoAssistente {
    static final long TOKENS_DO_RESUMO = 8_000;
    static final long TOKENS_DA_LEITURA = 4_000;
    static final long TOKENS_DAS_RECOMENDACOES = 6_000;

    private static final String CONTEXTO = """
            Você apoia a equipe de pós-venda de uma associação de proteção veicular no Brasil. Os dados pessoais foram \
            trocados por marcadores como [CLIENTE], [PLACA_1] e [TELEFONE_1]; mantenha-os exatamente como estão e não \
            tente descobrir quem são. Escreva em português do Brasil.""";

    static final String RESUMO = CONTEXTO + """

            Você recebe as anotações livres de um atendimento e o texto dos anexos. Responda só com um objeto JSON, \
            sem nenhum texto antes ou depois, com os campos:
            - "resumoCurto": resumo objetivo do atendimento, até 500 caracteres;
            - "assuntos": lista com um ou mais destes valores: %s;
            - "satisfacao": "POSITIVA", "NEUTRA" ou "NEGATIVA";
            - "objecoes": até 5 frases curtas com as objeções ou reclamações do cliente (lista vazia se não houver);
            - "interesses": até 5 frases curtas com interesses ou pedidos do cliente (lista vazia se não houver);
            - "riscoDeCancelamento": true se houver sinal de que o cliente pode cancelar, senão false;
            - "chanceDeIndicar": "BAIXA", "MEDIA" ou "ALTA";
            - "proximaAcao": a próxima ação sugerida para a equipe, até 500 caracteres.""".formatted(
            Arrays.stream(Assunto.values()).map(a -> "\"" + a.name() + "\"").collect(Collectors.joining(", ")));

    static final String LEITURA_DAS_TENDENCIAS = CONTEXTO + """

            Você recebe só números agregados da carteira num período (JSON), sem dados de nenhum cliente. Escreva uma \
            leitura curta, de até 6 frases, com as principais tendências e onde a equipe deveria agir primeiro. Use só \
            os números recebidos; não invente números nem percentuais que não possam ser calculados a partir deles. \
            Responda em texto corrido, sem títulos nem listas.""";

    static final String RECOMENDACOES = CONTEXTO + """

            Você recebe os resumos recentes de atendimentos de um cliente e os indicadores gerais da carteira. Sugira \
            até 3 abordagens para o próximo contato com esse cliente. São sugestões para a equipe decidir: nunca \
            proponha enviar mensagens automáticas nem alterar contrato. Responda só com um objeto JSON, sem nenhum \
            texto antes ou depois, no formato {"recomendacoes": [{"abordagem": "...", "base": "..."}]}, em que \
            "abordagem" é a sugestão (até 600 caracteres) e "base" explica em que ela se baseia.""";

    private InstrucoesDoAssistente() {
    }
}
