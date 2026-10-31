package br.com.superauto.crm.cadastro.repositorio;

/** Colunas pelas quais a carteira pode ser ordenada (CAR-01); o padrão é o nome. */
public enum OrdemCarteira {
    NOME("associadoNome"),
    PLACA("placa"),
    CIDADE("cidade"),
    MENSALIDADE("mensalidade"),
    SITUACAO("situacao"),
    ETAPA("etapaOrdem"),
    PROXIMO_CONTATO("proximoContatoEm"),
    VENDEDOR("vendedorNome");

    private final String atributo;

    OrdemCarteira(String atributo) {
        this.atributo = atributo;
    }

    /** Atributo do modelo de leitura {@code LinhaCarteira}. */
    public String atributo() {
        return atributo;
    }
}
