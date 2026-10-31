package br.com.superauto.crm.cadastro.entidade;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Tudo o que nasce com o contrato cadastrado na tela (CAR-02): a adesão, quem vendeu e os dados opcionais do SGA.
 *
 * @param vendidoEm data da venda, quando informada
 * @param codigoSga código do contrato no SGA, quando informado
 */
public record NovoContrato(Adesao adesao, UUID vendedorId, Optional<OffsetDateTime> vendidoEm,
                           Optional<String> codigoSga) {

    public NovoContrato {
        Objects.requireNonNull(adesao, "adesao");
        Objects.requireNonNull(vendedorId, "vendedorId");
        Objects.requireNonNull(vendidoEm, "vendidoEm");
        codigoSga = Objects.requireNonNull(codigoSga, "codigoSga").map(String::strip).filter(c -> !c.isEmpty());
    }
}
