package br.com.superauto.crm.cadastro.servico;

import java.util.Optional;
import java.util.UUID;

/**
 * Quanto da carteira a pessoa enxerga (RG-03): gerencial e atendente veem tudo; o vendedor, só os contratos em que é
 * o vendedor.
 *
 * @param vendedor preenchido só quando quem pergunta é vendedor
 */
public record EscopoCarteira(Optional<UUID> vendedor) {

    public static final EscopoCarteira TUDO = new EscopoCarteira(Optional.empty());

    public static EscopoCarteira doVendedor(UUID vendedor) {
        return new EscopoCarteira(Optional.of(vendedor));
    }

    public boolean enxergaContratoDe(Optional<UUID> vendedorDoContrato) {
        return vendedor.isEmpty() || vendedor.equals(vendedorDoContrato);
    }
}
