package br.com.superauto.crm.cadastro.repositorio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Um atendimento como a ficha mostra no histórico (CAR-03).
 *
 * @param contratoId vazio em contatos sobre a pessoa, como a mensagem de aniversário
 * @param corrigido já existe um registro de correção apontando para este (ATD-04)
 * @param corrigeAtendimentoId o atendimento que este registro corrige, se for uma correção
 */
public record AtendimentoDoHistorico(Long id, Optional<Long> contratoId, Optional<String> placa, String tipo,
                                     Optional<String> etapa, String resultado, Optional<String> descricao,
                                     Optional<LocalDate> retornoAgendadoPara, OffsetDateTime realizadoEm,
                                     String registradoPor, boolean corrigido, Optional<Long> corrigeAtendimentoId) {
}
