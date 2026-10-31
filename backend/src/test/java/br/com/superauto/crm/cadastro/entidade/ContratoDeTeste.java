package br.com.superauto.crm.cadastro.entidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Contratos prontos para os testes de serviço (fora do pacote da entidade). */
public final class ContratoDeTeste {

    private static final OffsetDateTime ADESAO = OffsetDateTime.parse("2026-03-13T00:00:00-03:00");

    private ContratoDeTeste() {
    }

    public static Contrato vigente(Long associadoId) {
        return doVendedor(associadoId, UUID.randomUUID());
    }

    public static Contrato doVendedor(Long associadoId, UUID vendedor) {
        var novo = new NovoContrato(new Adesao(associadoId, 2L, Mensalidade.de("124.82"), ADESAO), vendedor,
                Optional.empty(), Optional.empty());
        return Contrato.cadastrar(novo, LocalDate.of(2026, 10, 31), vendedor);
    }
}
