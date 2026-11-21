package br.com.superauto.crm.relacionamento.entidade;

/**
 * Tipos de atendimento, com os mesmos códigos da tabela {@code tipo_contato}. Só o pós-venda move a régua (ATD-01);
 * aniversário (ATD-03) e os avulsos (ATD-02) ficam apenas no histórico.
 */
public enum TipoDeRelacionamento {
    POS_VENDA(true, false),
    ANIVERSARIO(false, false),
    SUPORTE(false, true),
    COMERCIAL(false, true),
    REAPROXIMACAO(false, true),
    INDICACAO(false, true);

    private final boolean moveRegua;
    private final boolean avulso;

    TipoDeRelacionamento(boolean moveRegua, boolean avulso) {
        this.moveRegua = moveRegua;
        this.avulso = avulso;
    }

    public boolean moveRegua() {
        return moveRegua;
    }

    /** ATD-02: Suporte, Comercial, Reaproximação e Indicação. */
    public boolean avulso() {
        return avulso;
    }
}
