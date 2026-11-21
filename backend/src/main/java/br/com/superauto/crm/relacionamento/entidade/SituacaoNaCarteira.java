package br.com.superauto.crm.relacionamento.entidade;

/** Situação do contrato na projeção de relacionamento (RG-12). */
public enum SituacaoNaCarteira {
    A_CONFERIR,
    ATIVO,
    SUSPENSO,
    CANCELADO;

    public boolean permiteContato() {
        return this == A_CONFERIR || this == ATIVO;
    }

    /** ATD-01: contrato suspenso pode ser registrado pela ficha, como na macro; cancelado não. */
    public boolean aceitaPosVenda() {
        return this != CANCELADO;
    }

    public boolean pertenceACarteira() {
        return this != CANCELADO;
    }
}
