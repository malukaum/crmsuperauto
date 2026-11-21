package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * RG-03 · Os contratos que quem registra o atendimento enxerga: gerencial e atendente, todos; o vendedor, só os
 * seus. Fora do escopo, o contrato ou o associado responde como não encontrado.
 */
@Component
@RequiredArgsConstructor
public class CarteiraVisivel {
    private final TrabalhoRelacionamentoRepository trabalho;

    public ContratoNaFila contrato(Long contratoId, Optional<UUID> vendedor) {
        return trabalho.findById(contratoId)
                .filter(c -> vendedor.isEmpty() || vendedor.equals(Optional.ofNullable(c.getVendedorId())))
                .orElseThrow(() -> NegocioException.naoEncontrado("Contrato não encontrado."));
    }

    public List<ContratoNaFila> contratosDoAssociado(Long associadoId, Optional<UUID> vendedor) {
        var contratos = trabalho.dosAssociados(Set.of(associadoId), vendedor.orElse(null));
        if (contratos.isEmpty())
            throw NegocioException.naoEncontrado("Cliente não encontrado na carteira.");
        return contratos;
    }
}
