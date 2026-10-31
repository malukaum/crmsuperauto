package br.com.superauto.crm.cadastro.servico;

/**
 * Resultado da ativação em lote (CAR-05, proposta).
 *
 * @param ignorados contratos escolhidos que não estavam "a conferir" (ou não existem) e ficaram como estavam
 */
public record ResultadoAtivacao(int ativados, int ignorados) {
}
