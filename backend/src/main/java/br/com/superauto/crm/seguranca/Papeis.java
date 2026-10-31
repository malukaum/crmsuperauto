package br.com.superauto.crm.seguranca;

import java.util.List;

/**
 * Papéis do realm {@code crm} no Keycloak e as expressões usadas em {@code @PreAuthorize}.
 */
public final class Papeis {

    public static final String GERENCIAL = "gerencial";
    public static final String VENDEDOR = "vendedor";
    public static final String ATENDENTE = "atendente";
    public static final String CLIENTE = "cliente";

    /** Papéis da equipe interna, do mais amplo para o mais restrito. Contas de cliente não entram aqui. */
    public static final List<String> EQUIPE = List.of(GERENCIAL, VENDEDOR, ATENDENTE);

    public static final String SO_GERENCIAL = "hasRole('GERENCIAL')";
    public static final String GERENCIAL_OU_VENDEDOR = "hasAnyRole('GERENCIAL', 'VENDEDOR')";
    public static final String SO_CLIENTE = "hasRole('CLIENTE')";
    public static final String EQUIPE_INTERNA = "hasAnyRole('GERENCIAL', 'VENDEDOR', 'ATENDENTE')";

    private Papeis() {
    }
}
