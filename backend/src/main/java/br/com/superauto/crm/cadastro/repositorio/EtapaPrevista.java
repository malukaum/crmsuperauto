package br.com.superauto.crm.cadastro.repositorio;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Uma etapa da régua de um contrato como a ficha mostra (CAR-03): concluída, pendente (destacada) ou futura,
 * com a data prevista.
 *
 * @param prevista vazia para as etapas já concluídas e quando a régua não tem data
 */
public record EtapaPrevista(String codigo, String nome, SituacaoEtapa situacao, Optional<LocalDate> prevista) {

    public enum SituacaoEtapa {
        CONCLUIDA,
        PENDENTE,
        FUTURA
    }
}
