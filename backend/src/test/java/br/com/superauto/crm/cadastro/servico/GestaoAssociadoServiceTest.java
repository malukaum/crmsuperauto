package br.com.superauto.crm.cadastro.servico;

import static br.com.superauto.crm.Identificadores.comId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.Classificacao;
import br.com.superauto.crm.cadastro.entidade.ContratoDeTeste;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.DocumentoExibido;
import br.com.superauto.crm.cadastro.entidade.NovoAssociado;
import br.com.superauto.crm.cadastro.entidade.Perfil;
import br.com.superauto.crm.cadastro.entidade.PotencialIndicacao;
import br.com.superauto.crm.cadastro.repositorio.AssociadoRepository;
import br.com.superauto.crm.cadastro.repositorio.ContratoRepository;
import br.com.superauto.crm.cadastro.repositorio.ReferenciasDoCadastro;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class GestaoAssociadoServiceTest {

    private final AssociadoRepository associados = mock(AssociadoRepository.class);
    private final ContratoRepository contratos = mock(ContratoRepository.class);
    private final ReferenciasDoCadastro referencias = mock(ReferenciasDoCadastro.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final GestaoAssociadoService servico = new GestaoAssociadoService(associados, contratos, referencias,
            eventos, Clock.systemUTC());

    private final UUID vendedor = UUID.randomUUID();
    private final Autor gerente = new Autor(UUID.randomUUID(), EscopoCarteira.TUDO);
    private final Autor oVendedor = new Autor(vendedor, EscopoCarteira.doVendedor(vendedor));
    private Associado associado;

    @BeforeEach
    void setUp() {
        associado = comId(Associado.cadastrar(new NovoAssociado(Documento.de("52998224725"),
                new DadosPessoais("José", LocalDate.of(1980, 5, 10), 1), Telefone.de("37998692950"), Optional.empty()),
                vendedor), 7L);
        when(associados.findById(7L)).thenReturn(Optional.of(associado));
    }

    @Test
    @DisplayName("CAR-06: classificar salva o associado, o que publica o evento para a auditoria")
    void classificar() {
        var promotor = new Classificacao(Perfil.PROMOTOR, PotencialIndicacao.ALTO);

        servico.classificar(new FichaAberta(7L, 0), promotor, gerente);

        assertThat(associado.classificacao()).isEqualTo(promotor);
        verify(associados).save(associado);
    }

    @Test
    @DisplayName("RG-03: o vendedor só altera quem tem contrato dele")
    void vendedorSoNosSeusClientes() {
        when(contratos.existsByAssociadoIdAndVendedorId(7L, vendedor)).thenReturn(false);

        assertThatThrownBy(() -> servico.classificar(new FichaAberta(7L, 0), Classificacao.PADRAO, oVendedor))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.NAO_ENCONTRADO));
        verify(associados, never()).save(any());
    }

    @Test
    @DisplayName("RG-06: a segunda gravação sobre a mesma ficha é recusada")
    void edicaoSimultanea() {
        assertThatThrownBy(() -> servico.classificar(new FichaAberta(7L, 2), Classificacao.PADRAO, gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(NegocioException.CODIGO_EDICAO_SIMULTANEA));
    }

    @Test
    @DisplayName("RG-04: o documento completo é devolvido formatado e a exibição vai para a auditoria (proposta)")
    void exibirDocumento() {
        when(contratos.existsByAssociadoIdAndVendedorId(7L, vendedor)).thenReturn(true);

        assertThat(servico.exibirDocumento(7L, oVendedor)).isEqualTo("529.982.247-25");
        verify(eventos).publishEvent(new DocumentoExibido(7L, vendedor));
    }

    @Test
    @DisplayName("CAR-08: anonimizar confere os contratos da pessoa")
    void anonimizarComContratoVigente() {
        when(contratos.findByAssociadoId(7L)).thenReturn(List.of(ContratoDeTeste.vigente(7L)));

        assertThatThrownBy(() -> servico.anonimizar(new FichaAberta(7L, 0), gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("contrato-vigente"));
    }

    @Test
    @DisplayName("Corrigir os dados exige uma cidade da lista do IBGE")
    void cidadeInvalida() {
        when(referencias.municipio(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.corrigirDados(new FichaAberta(7L, 0), new DadosPessoais("José", null, 99), gerente))
                .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo("cidade-invalida"));
    }
}
