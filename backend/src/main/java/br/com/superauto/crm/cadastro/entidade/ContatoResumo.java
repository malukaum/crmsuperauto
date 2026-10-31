package br.com.superauto.crm.cadastro.entidade;

/** Retrato de um contato para eventos e auditoria. */
public record ContatoResumo(TipoContatoAssociado tipo, String valor, boolean principal, boolean aceitaContato) {
}
