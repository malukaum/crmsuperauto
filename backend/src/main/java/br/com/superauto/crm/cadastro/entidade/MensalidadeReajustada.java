package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/** Evento de domínio: a mensalidade mudou. O contrato guarda só o valor atual; o anterior fica na auditoria (CAR-05). */
public record MensalidadeReajustada(Long contratoId, Mensalidade anterior, Mensalidade nova, UUID autor) {
}
