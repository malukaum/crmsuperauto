package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Dados públicos da fila; documento e telefones de busca nunca saem na resposta. */
public record LinhaDeTrabalho(
        Long contratoId,
        Long associadoId,
        String nome,
        String whatsapp,
        boolean aceitaWhatsapp,
        String modelo,
        String placa,
        String tipoVeiculo,
        BigDecimal mensalidade,
        String cidade,
        String situacao,
        String etapa,
        LocalDate data,
        long diasAtraso,
        int versao,
        boolean mensagemEnviada) {
    public static LinhaDeTrabalho de(ContratoNaFila c, ExibicaoNaFila exibicao) {
        return new LinhaDeTrabalho(
                c.getContratoId(),
                c.getAssociadoId(),
                c.getNome(),
                c.getWhatsapp() == null ? null : c.getWhatsapp().valor(),
                Boolean.TRUE.equals(c.getAceitaWhatsapp()),
                c.getModelo(),
                c.getPlaca().valor(),
                c.getTipoVeiculo(),
                c.getMensalidade().valor(),
                c.getCidade(),
                c.getSituacao().name(),
                c.getEtapa(),
                exibicao.data(),
                exibicao.data() == null
                        ? 0
                        : Math.max(0, ChronoUnit.DAYS.between(exibicao.data(), exibicao.hoje())),
                c.getVersao(),
                exibicao.enviada());
    }
}
