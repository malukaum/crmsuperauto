package br.com.superauto.crm.cadastro.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.cadastro.repositorio.ConsultaCarteira;
import br.com.superauto.crm.cadastro.repositorio.FiltroCarteira;
import br.com.superauto.crm.cadastro.repositorio.ResultadoCarteira;
import br.com.superauto.crm.cadastro.repositorio.TermoBusca;
import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ConsultaCarteiraServiceTest {

    private final ConsultaCarteira carteira = mock(ConsultaCarteira.class);
    private final ConsultaCarteiraService servico = new ConsultaCarteiraService(carteira);

    @Test
    @DisplayName("RG-03: o vendedor só vê os próprios contratos, mesmo pedindo o filtro de outro vendedor")
    void vendedorSoOsSeus() {
        UUID vendedor = UUID.randomUUID();
        when(carteira.buscar(any(), any())).thenReturn(vazio());

        servico.buscar(filtroDoVendedor(UUID.randomUUID()), primeiraPagina(), EscopoCarteira.doVendedor(vendedor));

        assertThat(filtroUsado().vendedorId()).contains(vendedor);
    }

    @Test
    @DisplayName("CAR-01: o rodapé traz a quantidade de contratos e a soma das mensalidades do filtro")
    void rodape() {
        when(carteira.buscar(any(), any())).thenReturn(new ResultadoCarteira(
                new PageImpl<>(List.of(), Pageable.ofSize(10), 42), Mensalidade.de("5240.10")));

        var pagina = servico.buscar(filtroDoVendedor(null), primeiraPagina(), EscopoCarteira.TUDO);

        assertThat(pagina.totalContratos()).isEqualTo(42);
        assertThat(pagina.somaMensalidades()).isEqualByComparingTo("5240.10");
    }

    @Test
    @DisplayName("CAR-01: páginas só de 10, 25 ou 50 linhas")
    void tamanhoDePagina() {
        Assertions.assertThatThrownBy(() -> new PaginaSolicitada(0, 30, Ordenacao.PADRAO))
                .hasMessageContaining("10, 25 ou 50");
    }

    private FiltroCarteira filtroUsado() {
        var captor = ArgumentCaptor.forClass(FiltroCarteira.class);
        verify(carteira).buscar(captor.capture(), any());
        return captor.getValue();
    }

    private static ResultadoCarteira vazio() {
        return new ResultadoCarteira(new PageImpl<>(List.of()), Mensalidade.de("0"));
    }

    private static PaginaSolicitada primeiraPagina() {
        return new PaginaSolicitada(0, 25, Ordenacao.PADRAO);
    }

    static FiltroCarteira filtroDoVendedor(UUID vendedor) {
        return new FiltroCarteira(TermoBusca.de("haa-7216"), Optional.of(Fila.ATRASADO), Optional.of(2535),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.ofNullable(vendedor));
    }
}
