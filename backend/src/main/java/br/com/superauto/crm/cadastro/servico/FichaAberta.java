package br.com.superauto.crm.cadastro.servico;

/**
 * A ficha que a pessoa tinha aberta ao salvar: o associado e a versão que ela viu (RG-06). Se a versão mudou,
 * a gravação é recusada.
 */
public record FichaAberta(Long associadoId, int versao) {
}
