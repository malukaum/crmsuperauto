package br.com.superauto.crm.assistente.entidade;

/** IA-02 · Lista fechada de assuntos de um atendimento, para os indicadores serem comparáveis entre clientes. */
public enum Assunto {
    ASSISTENCIA_24H("Assistência 24h"),
    GUINCHO("Guincho"),
    MENSALIDADE("Mensalidade"),
    SINISTRO("Sinistro"),
    CANCELAMENTO("Cancelamento"),
    INDICACAO("Indicação"),
    VEICULO("Veículo"),
    DOCUMENTACAO("Documentação"),
    ATENDIMENTO("Qualidade do atendimento"),
    OUTRO("Outro");

    private final String descricao;

    Assunto(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
