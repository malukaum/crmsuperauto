package br.com.superauto.crm.relacionamento.infraestrutura.persistencia;

import br.com.superauto.crm.assistente.repositorio.AtendimentosParaAssistente;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O Relacionamento, dono dos atendimentos, atende a porta {@link AtendimentosParaAssistente} do Assistente. Para o
 * vendedor, o atendimento precisa ser de um contrato dele (ou, sem contrato, de um cliente dele), como no histórico.
 */
@Component
@RequiredArgsConstructor
public class AtendimentosParaAssistenteJpa implements AtendimentosParaAssistente {
    private final AtendimentoRepository atendimentos;
    private final TrabalhoRelacionamentoRepository trabalho;

    @Override
    public Optional<Long> associadoDoAtendimento(Long atendimentoId, Optional<UUID> vendedor) {
        return atendimentos.findById(atendimentoId)
                .filter(atendimento -> vendedor.map(v -> daCarteira(atendimento, v)).orElse(true))
                .map(Atendimento::getAssociadoId);
    }

    private boolean daCarteira(Atendimento atendimento, UUID vendedor) {
        return atendimento.getContratoId()
                .map(contrato -> trabalho.findById(contrato).filter(c -> Objects.equals(c.getVendedorId(), vendedor)).isPresent())
                .orElseGet(() -> !trabalho.dosAssociados(Set.of(atendimento.getAssociadoId()), vendedor).isEmpty());
    }
}
