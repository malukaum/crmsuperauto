package br.com.superauto.crm.cadastro.servico;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.compartilhado.entidade.Fila;

/**
 * Uma linha do arquivo exportado (CAR-07), já com os formatos da RG-02.
 *
 * @param documento completo só para o gerencial; mascarado para os demais
 * @param whatsappSemContato o WhatsApp principal tem opt-out (sai marcado no arquivo)
 */
public record LinhaExportada(String associado, String documento, String whatsapp, boolean whatsappSemContato,
                             String placa, String modelo, String tipoVeiculo, String cidade, String uf,
                             BigDecimal mensalidade, SituacaoContrato situacao, String etapaPendente,
                             LocalDate proximoContato, Fila fila, int diasAtraso, String vendedor) {
}
