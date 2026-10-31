package br.com.superauto.crm.cadastro.servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.compartilhado.entidade.Telefone;

/**
 * Uma linha da carteira na tela (CAR-01): um contrato (placa) com a pessoa.
 *
 * @param documento sempre mascarado na lista (RG-04)
 * @param whatsapp o principal, 55 + DDD + número; nulo se não houver
 */
public record LinhaDaCarteira(Long contratoId, Long associadoId, String associado, String documento, String placa,
                              String modelo, String tipoVeiculo, String cidade, String uf, BigDecimal mensalidade,
                              SituacaoContrato situacao, String etapaPendente, LocalDate proximoContato, Fila fila,
                              int diasAtraso, UUID vendedorId, String vendedorNome, String whatsapp,
                              boolean whatsappAceitaContato) {

    static LinhaDaCarteira de(LinhaCarteira linha) {
        return new LinhaDaCarteira(linha.getContratoId(), linha.getAssociadoId(), linha.getAssociadoNome(),
                linha.getDocumento().map(Documento::mascarado).orElse(null), linha.getPlaca().valor(),
                linha.getModelo(), linha.getTipoVeiculo(), linha.getCidade(), linha.getUf(),
                linha.getMensalidade().valor(), linha.getSituacao(), linha.getEtapaPendente().orElse(null),
                linha.getProximoContatoEm().orElse(null), linha.getFila(), linha.getDiasAtraso(),
                linha.getVendedorId().orElse(null), linha.getVendedorNome().orElse(null),
                linha.getWhatsapp().map(Telefone::valor).orElse(null), linha.whatsappAceitaContato());
    }
}
