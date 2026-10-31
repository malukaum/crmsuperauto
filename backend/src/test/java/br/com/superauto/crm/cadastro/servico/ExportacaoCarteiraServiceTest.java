package br.com.superauto.crm.cadastro.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.cadastro.entidade.CarteiraExportada;
import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.repositorio.ConsultaCarteira;
import br.com.superauto.crm.cadastro.repositorio.FiltroCarteira;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ExportacaoCarteiraServiceTest {

    private final ConsultaCarteira carteira = mock(ConsultaCarteira.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ExportacaoCarteiraService servico = new ExportacaoCarteiraService(carteira, eventos);
    private final UUID vendedor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var linhas = List.of(linha());
        when(carteira.listar(any(), any())).thenReturn(linhas);
    }

    @Test
    @DisplayName("CAR-07: dado o vendedor, quando exporta a carteira sem filtro, o arquivo tem só os contratos dele")
    void vendedorSoOsSeus() {
        servico.exportar(semFiltro(), Ordenacao.PADRAO, new Exportador(vendedor, EscopoCarteira.doVendedor(vendedor), false));

        var captor = ArgumentCaptor.forClass(FiltroCarteira.class);
        verify(carteira).listar(captor.capture(), any());
        assertThat(captor.getValue().vendedorId()).contains(vendedor);
    }

    @Test
    @DisplayName("CAR-07: o documento sai mascarado para o vendedor e completo para o gerencial (proposta)")
    void documento() {
        var doVendedor = servico.exportar(semFiltro(), Ordenacao.PADRAO,
                new Exportador(vendedor, EscopoCarteira.doVendedor(vendedor), false));
        var doGerente = servico.exportar(semFiltro(), Ordenacao.PADRAO,
                new Exportador(UUID.randomUUID(), EscopoCarteira.TUDO, true));

        assertThat(doVendedor.getFirst().documento()).isEqualTo("***.***.247-**");
        assertThat(doGerente.getFirst().documento()).isEqualTo("529.982.247-25");
    }

    @Test
    @DisplayName("CAR-07: contato com opt-out sai marcado")
    void optOut() {
        var linhas = servico.exportar(semFiltro(), Ordenacao.PADRAO, new Exportador(vendedor, EscopoCarteira.TUDO, true));

        assertThat(linhas.getFirst().whatsappSemContato()).isTrue();
        assertThat(linhas.getFirst().whatsapp()).isEqualTo("(37) 9 9869-2950");
    }

    @Test
    @DisplayName("CAR-07: cada exportação vai para a auditoria com o filtro usado e a quantidade de linhas")
    void auditoria() {
        servico.exportar(semFiltro(), Ordenacao.PADRAO, new Exportador(vendedor, EscopoCarteira.doVendedor(vendedor), false));

        verify(eventos).publishEvent(new CarteiraExportada(Map.of("vendedorId", vendedor.toString()), 1, vendedor));
    }

    private static FiltroCarteira semFiltro() {
        return new FiltroCarteira(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static LinhaCarteira linha() {
        var linha = mock(LinhaCarteira.class);
        when(linha.getAssociadoNome()).thenReturn("José da Silva");
        when(linha.getDocumento()).thenReturn(Optional.of(new Documento("52998224725")));
        when(linha.getWhatsapp()).thenReturn(Optional.of(Telefone.de("37998692950")));
        when(linha.whatsappAceitaContato()).thenReturn(false);
        when(linha.getPlaca()).thenReturn(Placa.de("HAA7216"));
        when(linha.getModelo()).thenReturn("Onix");
        when(linha.getTipoVeiculo()).thenReturn("Carro");
        when(linha.getMensalidade()).thenReturn(Mensalidade.de("124.82"));
        when(linha.getSituacao()).thenReturn(SituacaoContrato.ATIVO);
        when(linha.getEtapaPendente()).thenReturn(Optional.of("D+7"));
        when(linha.getProximoContatoEm()).thenReturn(Optional.empty());
        when(linha.getFila()).thenReturn(Fila.ATRASADO);
        when(linha.getVendedorNome()).thenReturn(Optional.of("Vendedor Um"));
        return linha;
    }
}
