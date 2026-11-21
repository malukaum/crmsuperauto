package br.com.superauto.crm.assistente.entidade;

/** IA-05 · Provedores de IA contratados. Provedor novo entra como mais um valor e mais um adaptador. */
public enum ProvedorDeIa {
    CLAUDE("Claude (Anthropic)"),
    GEMINI("Gemini (Google)"),
    CHATGPT("ChatGPT (OpenAI)");

    private final String nome;

    ProvedorDeIa(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }
}
