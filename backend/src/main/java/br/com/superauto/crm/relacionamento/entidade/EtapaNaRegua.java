package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Uma etapa da régua de um contrato, como fica para frente: concluída, pendente ou futura, com a data prevista.
 *
 * @param prevista vazia nas concluídas e quando não há data de próximo contato
 */
public record EtapaNaRegua(EtapaRegua etapa, Andamento andamento, Optional<LocalDate> prevista) {

    public enum Andamento {
        CONCLUIDA,
        PENDENTE,
        FUTURA
    }
}
