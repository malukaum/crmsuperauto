package br.com.superauto.crm.cadastro.servico;

import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * As condições do novo contrato (CAR-02).
 *
 * @param dataAdesao cadastro no SGA, base do primeiro contato da régua
 * @param vendedorId só para o gerencial, que escolhe o vendedor
 */
public record CondicoesContrato(Mensalidade mensalidade, LocalDate dataAdesao, Optional<LocalDate> dataVenda,
                                Optional<String> codigoSga, Optional<UUID> vendedorId) {
}
