package br.com.superauto.crm.assistente.entidade;

/**
 * IA-06 · O que identifica o cliente pelo nome e precisa sair do texto antes de ir ao provedor de IA. CPF/CNPJ,
 * placa, telefone e e-mail (dele ou de qualquer pessoa) são reconhecidos pelo formato, no próprio texto.
 */
public record DadosIdentificaveis(String nome) {

    public static final DadosIdentificaveis NENHUM = new DadosIdentificaveis(null);
}
