package br.com.superauto.crm.cadastro.entidade;

import static br.com.superauto.crm.Identificadores.comId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssociadoTest {

    private static final Documento CPF = new Documento("52998224725");
    private static final Documento CNPJ = new Documento("11222333000181");
    private static final Integer FORMIGA = 2535;
    private static final UUID AUTOR = UUID.randomUUID();
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-31T10:00:00-03:00");

    @Nested
    class Cadastro {

        @Test
        @DisplayName("CAR-02: nasce Regular, com potencial não avaliado e WhatsApp e e-mail como principais")
        void nasceComPadroes() {
            var associado = associado();

            assertThat(associado.classificacao()).isEqualTo(Classificacao.PADRAO);
            assertThat(associado.getContatos()).extracting(ContatoAssociado::isPrincipal).containsOnly(true);
            assertThat(associado.whatsappPrincipal()).map(c -> c.getMeio().valor()).contains("5537998692950");
        }

        @Test
        @DisplayName("Nascimento só vale para pessoa física")
        void nascimentoSoPessoaFisica() {
            var dados = new DadosPessoais("Transportes Ltda", LocalDate.of(1990, 1, 1), FORMIGA);
            var novo = new NovoAssociado(CNPJ, dados, Telefone.de("37998692950"), Optional.empty());

            assertThatThrownBy(() -> Associado.cadastrar(novo, AUTOR))
                    .isInstanceOfSatisfying(NegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("nascimento-pessoa-juridica"));
        }

        @Test
        @DisplayName("Cidade é obrigatória")
        void cidadeObrigatoria() {
            assertThatThrownBy(() -> new DadosPessoais("José", null, null))
                    .isInstanceOfSatisfying(NegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("cidade-obrigatoria"));
        }
    }

    @Nested
    class Contatos {

        @Test
        @DisplayName("CAR-04: o mesmo número repetido no mesmo cliente é recusado")
        void repetidoRecusado() {
            var associado = associado();

            assertThatThrownBy(() -> associado.adicionarContato(whatsapp("(37) 99869-2950", false), AUTOR))
                    .isInstanceOfSatisfying(NegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("contato-repetido"));
        }

        @Test
        @DisplayName("CAR-04: o mesmo número pode ser WhatsApp e telefone")
        void mesmoNumeroEmTiposDiferentes() {
            var associado = associado();

            associado.adicionarContato(new DadosContato(MeioContato.de(TipoContatoAssociado.TELEFONE, "37998692950"),
                    false, true), AUTOR);

            assertThat(associado.getContatos()).hasSize(3);
        }

        @Test
        @DisplayName("CAR-04: marcar outro WhatsApp como principal desmarca o anterior (um principal por tipo)")
        void umPrincipalPorTipo() {
            var associado = associado();

            associado.adicionarContato(whatsapp("37 98888-7777", true), AUTOR);

            assertThat(associado.whatsappPrincipal()).map(c -> c.getMeio().valor()).contains("5537988887777");
            assertThat(whatsapps(associado)).filteredOn(ContatoAssociado::isPrincipal).hasSize(1);
        }

        @Test
        @DisplayName("CAR-04: removendo o principal, o outro do mesmo tipo vira principal")
        void removerPrincipalPromoveOutro() {
            var associado = associado();
            associado.adicionarContato(whatsapp("37 98888-7777", false), AUTOR);
            numerar(associado);

            associado.removerContato(principal(associado).getId(), AUTOR);

            assertThat(associado.whatsappPrincipal()).map(c -> c.getMeio().valor()).contains("5537988887777");
        }

        @Test
        @DisplayName("CAR-04: o cliente precisa ter ao menos um WhatsApp ou telefone (proposta)")
        void precisaDeTelefone() {
            var associado = associado();
            numerar(associado);

            assertThatThrownBy(() -> associado.removerContato(principal(associado).getId(), AUTOR))
                    .isInstanceOfSatisfying(NegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("contato-telefonico-obrigatorio"));
        }

        @Test
        @DisplayName("CAR-04: opt-out mantém o número, marcado como não aceita contato")
        void optOut() {
            var associado = associado();
            numerar(associado);
            var whatsapp = principal(associado);

            associado.alterarContato(whatsapp.getId(), new DadosContato(whatsapp.getMeio(), true, false), AUTOR);

            assertThat(associado.whatsappPrincipal()).get()
                    .satisfies(c -> assertThat(c.isAceitaContato()).isFalse())
                    .satisfies(c -> assertThat(c.getMeio().valor()).isEqualTo("5537998692950"));
        }

        @Test
        @DisplayName("Toda mudança de contato gera evento com a lista antes e depois (RG-08)")
        void eventoComAntesEDepois() {
            var associado = comId(associado(), 7L);

            associado.adicionarContato(whatsapp("37 98888-7777", false), AUTOR);

            assertThat(associado.eventosPendentes()).singleElement().isInstanceOfSatisfying(ContatosAlterados.class, e -> {
                assertThat(e.anteriores()).hasSize(2);
                assertThat(e.atuais()).hasSize(3);
                assertThat(e.autor()).isEqualTo(AUTOR);
            });
        }
    }

    @Nested
    class Classificar {

        @Test
        @DisplayName("CAR-06: marcar como Promotor gera evento com o anterior e o novo")
        void promotor() {
            var associado = comId(associado(), 7L);
            var promotor = new Classificacao(Perfil.PROMOTOR, PotencialIndicacao.ALTO);

            associado.classificar(promotor, AUTOR);

            assertThat(associado.classificacao()).isEqualTo(promotor);
            assertThat(associado.eventosPendentes()).containsExactly(
                    new ClassificacaoAlterada(7L, Classificacao.PADRAO, promotor, AUTOR));
        }

        @Test
        @DisplayName("Classificar com os mesmos valores não gera evento")
        void semMudanca() {
            var associado = associado();

            associado.classificar(Classificacao.PADRAO, AUTOR);

            assertThat(associado.eventosPendentes()).isEmpty();
        }
    }

    @Nested
    class Anonimizar {

        @Test
        @DisplayName("CAR-08: só anonimiza quando todos os contratos estão cancelados (proposta)")
        void exigeTodosCancelados() {
            var associado = associado();

            assertThatThrownBy(() -> associado.anonimizar(List.of(ContratoTest.contratoAConferir()), AGORA, AUTOR))
                    .isInstanceOfSatisfying(NegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("contrato-vigente"));
        }

        @Test
        @DisplayName("CAR-08: nome vira \"Cliente anonimizado nº\" + código; documento, nascimento e contatos são apagados")
        void apagaDadosPessoais() {
            var associado = comId(associado(), 1042L);
            var cancelado = ContratoTest.contratoAConferir();
            cancelado.cancelar(AGORA, AUTOR);

            associado.anonimizar(List.of(cancelado), AGORA, AUTOR);

            assertThat(associado.getNome()).isEqualTo("Cliente anonimizado nº 1042");
            assertThat(associado.getDocumento()).isEmpty();
            assertThat(associado.getDataNascimento()).isEmpty();
            assertThat(associado.getContatos()).isEmpty();
            assertThat(associado.getMunicipioId()).isEqualTo(FORMIGA);
            assertThat(associado.eventosPendentes()).contains(new AssociadoAnonimizado(1042L, null, AUTOR));
        }

        @Test
        @DisplayName("CAR-08: é irreversível: cadastro anonimizado não é mais alterado")
        void irreversivel() {
            var associado = associado();
            associado.anonimizar(List.of(), AGORA, AUTOR);

            assertThatThrownBy(() -> associado.classificar(new Classificacao(Perfil.ATENCAO, PotencialIndicacao.BAIXO), AUTOR))
                    .isInstanceOfSatisfying(NegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("associado-anonimizado"));
        }
    }

    @Test
    @DisplayName("RG-06: salvar sobre uma versão diferente da vista é recusado")
    void edicaoSimultanea() {
        var associado = associado();

        assertThatThrownBy(() -> associado.conferirVersao(3))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(NegocioException.CODIGO_EDICAO_SIMULTANEA);
                    assertThat(e.getMessage()).isEqualTo(
                            "Este cadastro foi alterado por outra pessoa. Recarregue para ver a versão atual.");
                });
    }

    static Associado associado() {
        var dados = new DadosPessoais("José da Silva", LocalDate.of(1980, 5, 10), FORMIGA);
        return Associado.cadastrar(new NovoAssociado(CPF, dados, Telefone.de("(37) 99869-2950"),
                Optional.of(Email.de("jose@exemplo.com"))), AUTOR);
    }

    private static DadosContato whatsapp(String numero, boolean principal) {
        return new DadosContato(MeioContato.de(TipoContatoAssociado.WHATSAPP, numero), principal, true);
    }

    private static List<ContatoAssociado> whatsapps(Associado associado) {
        return associado.getContatos().stream().filter(c -> c.doTipo(TipoContatoAssociado.WHATSAPP)).toList();
    }

    private static ContatoAssociado principal(Associado associado) {
        return associado.whatsappPrincipal().orElseThrow();
    }

    /** Simula os ids que o banco daria aos contatos. */
    private static void numerar(Associado associado) {
        long id = 1;
        for (ContatoAssociado contato : associado.getContatos()) {
            comId(contato, id++);
        }
    }
}
