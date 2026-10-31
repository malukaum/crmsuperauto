package br.com.superauto.crm.cadastro.entidade;

import java.util.Set;

/**
 * Situação do contrato (RG-12), espelhando o {@code ck_contrato_situacao}, com as mudanças permitidas pela CAR-05:
 * de "A conferir" para Ativo, Suspenso ou Cancelado; entre Ativo e Suspenso; de qualquer uma para Cancelado.
 * Cancelado é definitivo (proposta): para voltar, cadastra-se um novo contrato.
 */
public enum SituacaoContrato {
    A_CONFERIR,
    ATIVO,
    SUSPENSO,
    CANCELADO;

    public boolean podeMudarPara(SituacaoContrato nova) {
        return destinosPermitidos().contains(nova);
    }

    private Set<SituacaoContrato> destinosPermitidos() {
        return switch (this) {
            case A_CONFERIR -> Set.of(ATIVO, SUSPENSO, CANCELADO);
            case ATIVO -> Set.of(SUSPENSO, CANCELADO);
            case SUSPENSO -> Set.of(ATIVO, CANCELADO);
            case CANCELADO -> Set.of();
        };
    }
}
