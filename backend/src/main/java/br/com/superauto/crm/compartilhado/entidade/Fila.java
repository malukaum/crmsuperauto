package br.com.superauto.crm.compartilhado.entidade;

/**
 * Em que fila o contrato está, calculado pela visão {@code vw_fila_contato} com o "hoje" de São Paulo (RG-14).
 * Núcleo compartilhado: as filas (relacionamento) e o filtro da carteira (cadastro) falam a mesma língua.
 */
public enum Fila {
    ATRASADO,
    HOJE,
    FUTURO,
    SEM_DATA,
    SUSPENSO,
    CANCELADO
}
