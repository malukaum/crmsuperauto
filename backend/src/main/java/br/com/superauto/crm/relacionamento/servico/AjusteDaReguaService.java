package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.AjusteDaRegua;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ATD-04 · O gerencial ajusta a etapa pendente e o retorno manual de um contrato depois de um atendimento lançado
 * errado (proposta). A mudança vai para a auditoria pelo evento do acompanhamento (RG-08).
 */
@Service
@RequiredArgsConstructor
public class AjusteDaReguaService {
    private final TrabalhoRelacionamentoRepository trabalho;
    private final AcompanhamentoContratoRepository acompanhamentos;
    private final ReguaDePosVenda regua;
    private final Clock relogio;

    @Transactional
    public void ajustar(Long contratoId, AjusteDaRegua ajuste, UUID autor) {
        var contrato =
                trabalho.findById(contratoId)
                        .orElseThrow(() -> NegocioException.naoEncontrado("Contrato não encontrado."));
        var acompanhamento =
                acompanhamentos
                        .findById(contratoId)
                        .orElseThrow(() -> NegocioException.naoEncontrado("Acompanhamento não encontrado."));
        acompanhamento.ajustarPelaGerencia(
                ajuste,
                new AcompanhamentoContrato.ContextoDoAjuste(
                        LocalDate.now(relogio), contrato.getBaseRegua(), regua.etapas()),
                autor);
        acompanhamentos.save(acompanhamento);
    }
}
