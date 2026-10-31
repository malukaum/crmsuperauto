package br.com.superauto.crm.auditoria.entidade;

/** Valores aceitos em {@code auditoria_evento.operacao} (ver {@code ck_auditoria_operacao}). */
public enum OperacaoAuditoria {
    CRIACAO,
    ALTERACAO,
    EXCLUSAO,
    VISUALIZACAO,
    EXPORTACAO,
    ANONIMIZACAO
}
