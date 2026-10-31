package br.com.superauto.crm.relacionamento.servico;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Dados do caso de uso de relacionamento. */
public record ResumoDoPainel(
        long carteira,
        Map<String, Long> porTipo,
        long contatosHoje,
        long atrasados,
        long aniversariantesHoje,
        long aniversariantes7Dias,
        BigDecimal mensalidadesAtivas,
        long atendimentosMes,
        long aConferir,
        Map<String, Long> situacoes,
        List<LinhaDeTrabalho> proximosContatos,
        List<LinhaDeTrabalho> proximosAniversariantes) {}
