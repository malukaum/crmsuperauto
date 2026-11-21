package br.com.superauto.crm.cadastro.infraestrutura.persistencia;

import br.com.superauto.crm.assistente.entidade.DadosIdentificaveis;
import br.com.superauto.crm.assistente.repositorio.CarteiraParaAssistente;
import br.com.superauto.crm.cadastro.repositorio.AssociadoRepository;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** O Cadastro, dono do associado e dos contratos, atende a porta {@link CarteiraParaAssistente} do Assistente. */
@Component
@RequiredArgsConstructor
public class CarteiraParaAssistenteJpa implements CarteiraParaAssistente {
    private final AssociadoRepository associados;
    private final ContratoRepository contratos;

    @Override
    public DadosIdentificaveis identificaveis(Long associadoId) {
        return associados.findById(associadoId)
                .map(associado -> new DadosIdentificaveis(associado.getNome()))
                .orElse(DadosIdentificaveis.NENHUM);
    }

    @Override
    public Set<Long> associadosDoVendedor(UUID vendedor) {
        return Set.copyOf(contratos.associadosDoVendedor(vendedor));
    }

    @Override
    public boolean associadoVisivel(Long associadoId, Optional<UUID> vendedor) {
        return associados.existsById(associadoId)
                && vendedor.map(v -> contratos.existsByAssociadoIdAndVendedorId(associadoId, v)).orElse(true);
    }
}
