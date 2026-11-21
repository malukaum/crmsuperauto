package br.com.superauto.crm.assistente.entidade;

/** IA-05 · Tarefas do assistente; cada uma tem um provedor principal e um reserva. */
public enum TarefaDeIa {
    RESUMO("Resumo do atendimento"),
    TENDENCIAS("Leitura das tendências"),
    RECOMENDACOES("Recomendações de abordagem");

    private final String nome;

    TarefaDeIa(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }
}
