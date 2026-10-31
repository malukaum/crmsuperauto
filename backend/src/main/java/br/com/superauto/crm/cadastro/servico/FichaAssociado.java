package br.com.superauto.crm.cadastro.servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.com.superauto.crm.cadastro.entidade.Perfil;
import br.com.superauto.crm.cadastro.entidade.PotencialIndicacao;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.entidade.TipoContatoAssociado;
import br.com.superauto.crm.cadastro.repositorio.AtendimentoDoHistorico;
import br.com.superauto.crm.cadastro.repositorio.EtapaPrevista;
import br.com.superauto.crm.cadastro.repositorio.IndicacaoFeita;
import br.com.superauto.crm.compartilhado.entidade.Fila;

/**
 * CAR-03 · Tudo sobre o associado numa tela: dados da pessoa, contatos, contratos com a régua de cada um,
 * histórico de atendimentos e indicações feitas.
 *
 * @param documento sempre mascarado (RG-04); o completo é pedido à parte, com registro na auditoria
 * @param versao para a edição simultânea (RG-06)
 */
public record FichaAssociado(Long id, String nome, String documento, boolean pessoaFisica, LocalDate dataNascimento,
                             Integer municipioId, String cidade, String uf, Perfil perfil,
                             PotencialIndicacao potencial, boolean anonimizado, int versao,
                             List<ContatoDaFicha> contatos, List<ContratoDaFicha> contratos,
                             List<AtendimentoDoHistorico> atendimentos, List<IndicacaoFeita> indicacoes) {

    /** @param exibicao no formato da RG-02, ex.: (37) 9 9869-2950 */
    public record ContatoDaFicha(Long id, TipoContatoAssociado tipo, String valor, String exibicao, boolean principal,
                                 boolean aceitaContato) {
    }

    /** Um contrato da pessoa, com a fila e a régua (etapa pendente destacada e datas previstas). */
    public record ContratoDaFicha(Long id, int versao, String placa, String modelo, Short tipoVeiculoId,
                                  String tipoVeiculo, BigDecimal mensalidade, SituacaoContrato situacao,
                                  String codigoSga, LocalDate aderidoEm, LocalDate vendidoEm, LocalDate encerradoEm,
                                  UUID vendedorId, String vendedorNome, Fila fila, int diasAtraso,
                                  String etapaPendente, LocalDate proximoContato, List<EtapaPrevista> regua) {
    }
}
