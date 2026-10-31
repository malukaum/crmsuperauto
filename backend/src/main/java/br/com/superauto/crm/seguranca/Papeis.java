package br.com.superauto.crm.seguranca;

/**
 * Papéis do realm {@code crm} no Keycloak e as expressões usadas em {@code @PreAuthorize}.
 */
public final class Papeis {

    public static final String GERENCIAL = "gerencial";
    public static final String VENDEDOR = "vendedor";
    public static final String ATENDENTE = "atendente";
    public static final String CLIENTE = "cliente";

    public static final String SO_GERENCIAL = "hasRole('GERENCIAL')";
    public static final String SO_CLIENTE = "hasRole('CLIENTE')";
    public static final String EQUIPE_INTERNA = "hasAnyRole('GERENCIAL', 'VENDEDOR', 'ATENDENTE')";

    private Papeis() {
    }
}
