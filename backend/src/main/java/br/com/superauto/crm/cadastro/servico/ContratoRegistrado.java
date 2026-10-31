package br.com.superauto.crm.cadastro.servico;

/**
 * Resultado do cadastro: o contrato criado e a pessoa dele, para a tela abrir a ficha.
 *
 * @param associadoNovo falso quando a pessoa já estava na carteira e só ganhou um contrato
 */
public record ContratoRegistrado(Long contratoId, Long associadoId, boolean associadoNovo) {
}
