package br.com.superauto.crm.cadastro.servico;

import static br.com.superauto.crm.Identificadores.comId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;
import br.com.superauto.crm.cadastro.entidade.NovoAssociado;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.repositorio.AssociadoRepository;
import br.com.superauto.crm.cadastro.repositorio.AtendimentoDoHistorico;
import br.com.superauto.crm.cadastro.repositorio.ConsultaCarteira;
import br.com.superauto.crm.cadastro.repositorio.EtapaPrevista;
import br.com.superauto.crm.cadastro.repositorio.EtapaPrevista.SituacaoEtapa;
import br.com.superauto.crm.cadastro.repositorio.HistoricoDeAtendimentos;
import br.com.superauto.crm.cadastro.repositorio.IndicacoesDoAssociado;
import br.com.superauto.crm.cadastro.repositorio.ReferenciasDoCadastro;
import br.com.superauto.crm.cadastro.repositorio.ReguaDosContratos;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.erro.NegocioException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsultaAssociadoServiceTest {

    private final AssociadoRepository associados = mock(AssociadoRepository.class);
    private final ConsultaCarteira carteira = mock(ConsultaCarteira.class);
    private final ReferenciasDoCadastro referencias = mock(ReferenciasDoCadastro.class);
    private final ReguaDosContratos reguas = mock(ReguaDosContratos.class);
    private final HistoricoDeAtendimentos historico = mock(HistoricoDeAtendimentos.class);
    private final IndicacoesDoAssociado indicacoes = mock(IndicacoesDoAssociado.class);
    private final ConsultaAssociadoService servico = new ConsultaAssociadoService(associados, carteira, referencias,
            reguas, historico, indicacoes, Clock.system(ZoneId.of("America/Sao_Paulo")));

    private final UUID vendedor1 = UUID.randomUUID();
    private final UUID vendedor2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var associado = comId(Associado.cadastrar(new NovoAssociado(new Documento("52998224725"),
                new DadosPessoais("José da Silva", null, 1), Telefone.de("37998692950"), Optional.empty()),
                vendedor1), 7L);
        when(associados.findById(7L)).thenReturn(Optional.of(associado));
        when(referencias.municipio(1)).thenReturn(Optional.empty());
        var linhas = List.of(linha(100L, "HAA7216", vendedor1), linha(101L, "BRA2E19", vendedor2));
        when(carteira.doAssociado(7L)).thenReturn(linhas);
        when(reguas.previsoes(any())).thenAnswer(i -> Map.of(
                100L, List.of(pendente()), 101L, List.of(pendente())));
        when(historico.doAssociado(7L)).thenReturn(List.of(atendimento(1L, 100L), atendimento(2L, 101L), aniversario(3L)));
        when(indicacoes.feitasPor(7L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("CAR-03: dado um cliente com duas placas, a ficha mostra os dois contratos, cada um com sua régua")
    void duasPlacas() {
        var ficha = servico.ficha(7L, EscopoCarteira.TUDO);

        assertThat(ficha.contratos()).extracting(FichaAssociado.ContratoDaFicha::placa).containsExactly("HAA7216", "BRA2E19");
        assertThat(ficha.contratos()).allSatisfy(c -> assertThat(c.regua()).hasSize(1));
    }

    @Test
    @DisplayName("CAR-03: dado o atendente, quando abre uma ficha, o documento aparece mascarado")
    void documentoMascarado() {
        assertThat(servico.ficha(7L, EscopoCarteira.TUDO).documento()).isEqualTo("***.***.247-**");
    }

    @Test
    @DisplayName("RG-03: o vendedor só vê os próprios contratos, os atendimentos deles e os contatos sobre a pessoa")
    void vendedorSoOsSeus() {
        var ficha = servico.ficha(7L, EscopoCarteira.doVendedor(vendedor1));

        assertThat(ficha.contratos()).extracting(FichaAssociado.ContratoDaFicha::id).containsExactly(100L);
        assertThat(ficha.atendimentos()).extracting(AtendimentoDoHistorico::id).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("RG-03: o vendedor não abre a ficha de quem não tem contrato dele")
    void vendedorSemContrato() {
        assertThatThrownBy(() -> servico.ficha(7L, EscopoCarteira.doVendedor(UUID.randomUUID())))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.NAO_ENCONTRADO));
    }

    private static LinhaCarteira linha(Long contratoId, String placa, UUID vendedor) {
        var linha = mock(LinhaCarteira.class);
        when(linha.getContratoId()).thenReturn(contratoId);
        when(linha.getPlaca()).thenReturn(Placa.de(placa));
        when(linha.getMensalidade()).thenReturn(Mensalidade.de("124.82"));
        when(linha.getSituacao()).thenReturn(SituacaoContrato.ATIVO);
        when(linha.getAderidoEm()).thenReturn(OffsetDateTime.parse("2026-03-13T00:00:00-03:00"));
        when(linha.getVendedorId()).thenReturn(Optional.of(vendedor));
        when(linha.getFila()).thenReturn(Fila.FUTURO);
        return linha;
    }

    private static EtapaPrevista pendente() {
        return new EtapaPrevista("D7", "D+7", SituacaoEtapa.PENDENTE, Optional.of(LocalDate.of(2026, 3, 20)));
    }

    private static AtendimentoDoHistorico atendimento(Long id, Long contratoId) {
        return new AtendimentoDoHistorico(id, Optional.of(contratoId), Optional.empty(), "Pós-venda", Optional.empty(),
                "CONCLUIDO", Optional.empty(), Optional.empty(), OffsetDateTime.now(), "Fulano", false, Optional.empty());
    }

    private static AtendimentoDoHistorico aniversario(Long id) {
        return new AtendimentoDoHistorico(id, Optional.empty(), Optional.empty(), "Aniversário", Optional.empty(),
                "CONCLUIDO", Optional.empty(), Optional.empty(), OffsetDateTime.now(), "Fulano", false, Optional.empty());
    }
}
