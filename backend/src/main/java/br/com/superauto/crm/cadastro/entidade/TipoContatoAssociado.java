package br.com.superauto.crm.cadastro.entidade;

/** Meio de falar com o associado. Valores de {@code ck_contato_associado_tipo}. */
public enum TipoContatoAssociado {
    WHATSAPP,
    TELEFONE,
    EMAIL;

    public boolean telefonico() {
        return this != EMAIL;
    }
}
