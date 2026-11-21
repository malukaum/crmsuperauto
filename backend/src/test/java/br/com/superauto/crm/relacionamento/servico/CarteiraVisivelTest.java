package br.com.superauto.crm.relacionamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFilaDeTeste;
import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.repositorio.ContagemDaCarteira;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class CarteiraVisivelTest {
    private static final UUID VENDEDOR = UUID.randomUUID();
    private static final ContratoNaFila DO_VENDEDOR =
            ContratoNaFilaDeTeste.contrato(10L, 7L, SituacaoNaCarteira.ATIVO, VENDEDOR);
    private final CarteiraVisivel carteira = new CarteiraVisivel(new TrabalhoEmMemoria(List.of(DO_VENDEDOR)));

    @Test
    @DisplayName("RG-03: o vendedor enxerga o próprio contrato; gerencial e atendente, todos")
    void enxergaNoEscopo() {
        assertThat(carteira.contrato(10L, Optional.of(VENDEDOR))).isSameAs(DO_VENDEDOR);
        assertThat(carteira.contrato(10L, Optional.empty())).isSameAs(DO_VENDEDOR);
    }

    @Test
    @DisplayName("RG-03: contrato e cliente de outro vendedor respondem como não encontrados")
    void foraDoEscopo() {
        var outro = Optional.of(UUID.randomUUID());

        assertThatThrownBy(() -> carteira.contrato(10L, outro))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Contrato não encontrado.");
        assertThatThrownBy(() -> carteira.contratosDoAssociado(7L, outro))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Cliente não encontrado na carteira.");
    }

    /** Dublê em memória da visão de trabalho, filtrando como a consulta real. */
    private record TrabalhoEmMemoria(List<ContratoNaFila> contratos) implements TrabalhoRelacionamentoRepository {
        @Override
        public Optional<ContratoNaFila> findById(Long id) {
            return contratos.stream().filter(c -> c.getContratoId().equals(id)).findFirst();
        }

        @Override
        public List<ContratoNaFila> dosAssociados(Set<Long> associados, UUID vendedor) {
            return contratos.stream()
                    .filter(c -> associados.contains(c.getAssociadoId()))
                    .filter(c -> vendedor == null || Objects.equals(vendedor, c.getVendedorId()))
                    .toList();
        }

        @Override
        public List<ContagemDaCarteira> contagens(UUID vendedor) {
            return List.of();
        }

        @Override
        public List<ContratoNaFila> comReguaAtiva() {
            return contratos;
        }
    }
}
