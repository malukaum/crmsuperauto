package br.com.superauto.crm.assistente.repositorio;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;

import java.math.BigDecimal;

/** IA-05 · Uso de um provedor no período: chamadas, falhas, vezes como reserva e custo estimado. */
public record UsoPorProvedor(ProvedorDeIa provedor, long chamadas, long falhas, long comoReserva, BigDecimal custo) {}
