package br.com.superauto.crm.cadastro.servico;

import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * CAR-05 · O contrato como o gerencial quer deixá-lo, conforme o SGA.
 *
 * @param versao a versão que o gerencial viu (RG-06)
 * @param codigoSga vazio retira o código
 */
public record AlteracaoContrato(int versao, SituacaoContrato situacao, Mensalidade mensalidade, UUID vendedorId,
                                Optional<String> codigoSga) {

    public AlteracaoContrato {
        Objects.requireNonNull(situacao, "situacao");
        Objects.requireNonNull(mensalidade, "mensalidade");
        Objects.requireNonNull(vendedorId, "vendedorId");
        Objects.requireNonNull(codigoSga, "codigoSga");
    }
}
