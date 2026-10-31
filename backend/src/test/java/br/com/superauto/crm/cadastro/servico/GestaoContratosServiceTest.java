package br.com.superauto.crm.cadastro.servico;

import static br.com.superauto.crm.Identificadores.comId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.cadastro.entidade.Contrato;
import br.com.superauto.crm.cadastro.entidade.ContratoDeTeste;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;
import br.com.superauto.crm.cadastro.repositorio.VendedoresDaEquipe;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.erro.NegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GestaoContratosServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-31T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private final ContratoRepository contratos = mock(ContratoRepository.class);
    private final VendedoresDaEquipe vendedores = mock(VendedoresDaEquipe.class);
    private final GestaoContratosService servico = new GestaoContratosService(contratos, vendedores, RELOGIO);
    private final UUID vendedor = UUID.randomUUID();
    private final UUID gerente = UUID.randomUUID();

    @Test
    @DisplayName("CAR-05: reajuste guarda o valor novo; o contrato é salvo para publicar o evento da auditoria")
    void reajuste() {
        Contrato contrato = carregado();

        servico.alterar(10L, alteracao(SituacaoContrato.A_CONFERIR, "139.90", vendedor), gerente);

        assertThat(contrato.getMensalidade()).isEqualTo(Mensalidade.de("139.90"));
        verify(contratos).save(contrato);
        verify(vendedores, never()).exigirVendedorAtivo(any());
    }

    @Test
    @DisplayName("CAR-05: cancelar grava a data de encerramento (hoje em São Paulo)")
    void cancelar() {
        Contrato contrato = carregado();

        servico.alterar(10L, alteracao(SituacaoContrato.CANCELADO, "124.82", vendedor), gerente);

        assertThat(contrato.getSituacao()).isEqualTo(SituacaoContrato.CANCELADO);
        assertThat(contrato.getEncerradoEm()).isNotNull();
    }

    @Test
    @DisplayName("CAR-05: trocar o vendedor exige um vendedor ativo e muda na hora quem enxerga o contrato")
    void trocarVendedor() {
        Contrato contrato = carregado();
        UUID novo = UUID.randomUUID();

        servico.alterar(10L, alteracao(SituacaoContrato.ATIVO, "124.82", novo), gerente);

        verify(vendedores).exigirVendedorAtivo(novo);
        assertThat(contrato.getVendedorId()).isEqualTo(novo);
    }

    @Test
    @DisplayName("RG-06: contrato alterado por outra pessoa desde que a tela abriu é recusado")
    void edicaoSimultanea() {
        carregado();
        var desatualizada = new AlteracaoContrato(5, SituacaoContrato.ATIVO, Mensalidade.de("1"), vendedor, Optional.empty());

        assertThatThrownBy(() -> servico.alterar(10L, desatualizada, gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(NegocioException.CODIGO_EDICAO_SIMULTANEA));
        verify(contratos, never()).save(any());
    }

    @Test
    @DisplayName("CAR-05: código do SGA já usado em outro contrato é recusado")
    void codigoSgaRepetido() {
        carregado();
        when(contratos.existsByCodigoSgaAndIdNot("SGA-1", 10L)).thenReturn(true);
        var alteracao = new AlteracaoContrato(0, SituacaoContrato.ATIVO, Mensalidade.de("124.82"), vendedor,
                Optional.of("SGA-1"));

        assertThatThrownBy(() -> servico.alterar(10L, alteracao, gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("codigo-sga-repetido"));
    }

    @Test
    @DisplayName("CAR-05: a ativação em lote só ativa os contratos \"a conferir\" (proposta)")
    void ativacaoEmLote() {
        var aConferir = comId(ContratoDeTeste.doVendedor(1L, vendedor), 1L);
        var suspenso = comId(ContratoDeTeste.doVendedor(2L, vendedor), 2L);
        suspenso.alterarSituacao(SituacaoContrato.SUSPENSO, null, gerente);
        when(contratos.findAllById(Set.of(1L, 2L))).thenReturn(List.of(aConferir, suspenso));

        var resultado = servico.ativarEmLote(List.of(1L, 2L, 2L), gerente);

        assertThat(resultado).isEqualTo(new ResultadoAtivacao(1, 1));
        assertThat(aConferir.getSituacao()).isEqualTo(SituacaoContrato.ATIVO);
        assertThat(suspenso.getSituacao()).isEqualTo(SituacaoContrato.SUSPENSO);
    }

    private Contrato carregado() {
        var contrato = comId(ContratoDeTeste.doVendedor(1L, vendedor), 10L);
        when(contratos.findById(10L)).thenReturn(Optional.of(contrato));
        return contrato;
    }

    private static AlteracaoContrato alteracao(SituacaoContrato situacao, String mensalidade, UUID vendedor) {
        return new AlteracaoContrato(0, situacao, Mensalidade.de(mensalidade), vendedor, Optional.empty());
    }
}
