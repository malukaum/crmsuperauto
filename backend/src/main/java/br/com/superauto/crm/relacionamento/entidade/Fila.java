package br.com.superauto.crm.relacionamento.entidade;

/** Em que fila o contrato está, calculado pela visão {@code vw_fila_contato} com o "hoje" de São Paulo. */
public enum Fila {
    ATRASADO,
    HOJE,
    FUTURO,
    SEM_DATA,
    SUSPENSO,
    CANCELADO
}
