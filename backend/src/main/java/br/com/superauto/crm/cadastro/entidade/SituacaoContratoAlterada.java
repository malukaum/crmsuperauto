package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/** Evento de domínio: o contrato mudou de situação (CAR-05). */
public record SituacaoContratoAlterada(Long contratoId, SituacaoContrato anterior, SituacaoContrato nova, UUID autor) {
}
