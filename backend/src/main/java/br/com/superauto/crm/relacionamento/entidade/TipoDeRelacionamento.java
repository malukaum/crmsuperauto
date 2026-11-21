package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

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

    /** ATD-02: só Suporte, Comercial, Reaproximação e Indicação são registrados como avulsos. */
    public void exigirAvulso() {
        if (!avulso)
            throw NegocioException.invalido(
                    "tipo-nao-avulso", "Escolha Suporte, Comercial, Reaproximação ou Indicação.");
    }
}
