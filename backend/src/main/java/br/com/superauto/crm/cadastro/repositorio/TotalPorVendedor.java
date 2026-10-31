package br.com.superauto.crm.cadastro.repositorio;

import java.util.UUID;

/** Projeção: quantos contratos um vendedor tem. */
public record TotalPorVendedor(UUID vendedorId, long total) {
}
