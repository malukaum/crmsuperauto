package br.com.superauto.crm.relacionamento.entidade;

/** Resultado de um atendimento (ATD-01), espelhando o CHECK de {@code atendimento.resultado}. */
public enum ResultadoContato {
    CONCLUIDO("Concluído"),
    SEM_RESPOSTA("Sem resposta"),
    REAGENDADO("Reagendado"),
    PENDENTE("Pendente");

    private final String descricao;

    ResultadoContato(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
