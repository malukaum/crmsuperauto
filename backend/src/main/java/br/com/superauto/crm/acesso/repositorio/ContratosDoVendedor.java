package br.com.superauto.crm.acesso.repositorio;

import java.util.Map;
import java.util.UUID;

/**
 * Contratos vigentes (não cancelados) por vendedor, e a transferência deles quando o vendedor é desativado.
 * Contratos cancelados continuam com o vendedor original, como histórico.
 */
public interface ContratosDoVendedor {

    Map<UUID, Long> contarPorVendedor();

    long contar(UUID vendedor);

    /** Passa os contratos vigentes de um vendedor para outro, registrando cada troca na auditoria. */
    int transferir(UUID de, UUID para, UUID autor);
}
