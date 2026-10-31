package br.com.superauto.crm.cadastro.servico;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;
import br.com.superauto.crm.cadastro.repositorio.VendedoresDaEquipe;
import br.com.superauto.crm.erro.NegocioException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CAR-05 · Alterar situação, mensalidade, vendedor e código do SGA do contrato (só o gerencial). As regras de
 * transição e de reajuste ficam no agregado {@link Contrato}; cada mudança sai como evento para a auditoria.
 */
@Service
public class GestaoContratosService {

    private final ContratoRepository contratos;
    private final VendedoresDaEquipe vendedores;
    private final Clock relogio;

    public GestaoContratosService(ContratoRepository contratos, VendedoresDaEquipe vendedores, Clock relogio) {
        this.contratos = contratos;
        this.vendedores = vendedores;
        this.relogio = relogio;
    }

    @Transactional
    public void alterar(Long contratoId, AlteracaoContrato alteracao, UUID autor) {
        Contrato contrato = contratos.findById(contratoId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Contrato não encontrado."));
        contrato.conferirVersao(alteracao.versao());
        if (!alteracao.vendedorId().equals(contrato.getVendedorId())) {
            vendedores.exigirVendedorAtivo(alteracao.vendedorId());
        }
        exigirCodigoSgaLivre(contrato, alteracao);
        // Primeiro o que exige contrato vigente; por último a situação, que pode cancelá-lo.
        contrato.reajustarMensalidade(alteracao.mensalidade(), autor);
        contrato.transferirPara(alteracao.vendedorId(), autor);
        contrato.alterarCodigoSga(alteracao.codigoSga(), autor);
        contrato.alterarSituacao(alteracao.situacao(), OffsetDateTime.now(relogio), autor);
        contratos.save(contrato);
    }

    /** Os importados chegam "a conferir": o gerencial confirma vários de uma vez como ativos (proposta). */
    @Transactional
    public ResultadoAtivacao ativarEmLote(Collection<Long> contratoIds, UUID autor) {
        Set<Long> escolhidos = Set.copyOf(contratoIds);
        List<Contrato> aConferir = contratos.findAllById(escolhidos).stream().filter(Contrato::aConferir).toList();
        var agora = OffsetDateTime.now(relogio);
        aConferir.forEach(contrato -> contrato.alterarSituacao(SituacaoContrato.ATIVO, agora, autor));
        contratos.saveAll(aConferir);
        return new ResultadoAtivacao(aConferir.size(), escolhidos.size() - aConferir.size());
    }

    private void exigirCodigoSgaLivre(Contrato contrato, AlteracaoContrato alteracao) {
        alteracao.codigoSga().map(String::strip).filter(c -> !c.isEmpty())
                .filter(codigo -> contratos.existsByCodigoSgaAndIdNot(codigo, contrato.getId()))
                .ifPresent(codigo -> {
                    throw NegocioException.conflito("codigo-sga-repetido",
                            "O código do SGA " + codigo + " já está em outro contrato.");
                });
    }
}
