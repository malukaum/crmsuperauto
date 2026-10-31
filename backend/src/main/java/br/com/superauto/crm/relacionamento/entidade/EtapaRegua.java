package br.com.superauto.crm.relacionamento.entidade;

/**
 * Uma etapa da régua de pós-venda como o Relacionamento a usa (RG-13).
 *
 * @param diasAposAnterior prazo contado do contato anterior concluído, ou da adesão na etapa inicial
 * @param proximaEtapaId etapa que fica pendente quando esta é concluída (o Trimestral aponta para si mesmo)
 */
public record EtapaRegua(Short id, String codigo, String nome, int diasAposAnterior, Short proximaEtapaId,
                         boolean inicial) {
}
