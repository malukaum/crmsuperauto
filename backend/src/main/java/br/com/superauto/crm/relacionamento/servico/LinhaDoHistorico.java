package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * ATD-05 · Uma linha do histórico de atendimentos, com as colunas da planilha.
 *
 * @param realizadoEm no fuso de São Paulo (RG-01)
 * @param proximaAcao calculada como na macro
 * @param corrigido já existe um registro de correção para este (ATD-04)
 */
public record LinhaDoHistorico(
        Long id,
        Long associadoId,
        String associado,
        String cidade,
        String placa,
        TipoDeRelacionamento tipoCodigo,
        String tipo,
        String etapa,
        ResultadoContato resultado,
        String descricao,
        String proximaAcao,
        LocalDate retornoAgendadoPara,
        OffsetDateTime realizadoEm,
        UUID registradoPorId,
        String registradoPor,
        boolean corrigido,
        Long corrigeAtendimentoId) {

    static LinhaDoHistorico de(LinhaHistoricoAtendimento linha, ZoneId fuso) {
        return new LinhaDoHistorico(
                linha.getId(),
                linha.getAssociadoId(),
                linha.getAssociadoNome(),
                linha.getCidade().orElse(null),
                linha.getPlaca().orElse(null),
                linha.getTipoCodigo(),
                linha.getTipo(),
                linha.getEtapa().orElse(null),
                linha.getResultado(),
                linha.getDescricao().orElse(null),
                linha.proximaAcao().descricao(),
                linha.getRetornoAgendadoPara().orElse(null),
                linha.getRealizadoEm().atZoneSameInstant(fuso).toOffsetDateTime(),
                linha.getRegistradoPorId(),
                linha.getRegistradoPor(),
                linha.isCorrigido(),
                linha.getCorrigeAtendimentoId().orElse(null));
    }
}
