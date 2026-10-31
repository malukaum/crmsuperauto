package br.com.superauto.crm.cadastro.entidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.DocumentoConverter;
import br.com.superauto.crm.erro.NegocioException;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;

/**
 * Raiz do agregado Associado: a pessoa física ou jurídica da carteira, cadastrada uma só vez por documento,
 * com os contatos dela ({@link ContatoAssociado}). Contratos e veículos são outros agregados.
 *
 * <p>Toda mudança sai como evento de domínio para a auditoria (RG-08), publicado no {@code save}.
 */
@Entity
@Table(name = "associado")
public class Associado extends AbstractAggregateRoot<Associado> {

    static final String PREFIXO_ANONIMIZADO = "Cliente anonimizado nº ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = DadosPessoais.TAMANHO_MAXIMO_NOME)
    private String nome;

    @Convert(converter = DocumentoConverter.class)
    @Column(length = 14, unique = true)
    private Documento documento;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "municipio_id")
    private Integer municipioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "potencial_indicacao", nullable = false, length = 20)
    private PotencialIndicacao potencialIndicacao;

    /** Conta na área do cliente (outro contexto): só o id. */
    @Column(name = "usuario_id", unique = true)
    private UUID usuarioId;

    @Column(name = "anonimizado_em")
    private OffsetDateTime anonimizadoEm;

    /** Unidirecional e dona da chave: mexer nos contatos incrementa a versão do associado (RG-06). */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "associado_id", nullable = false, updatable = false)
    @OrderBy("id")
    private List<ContatoAssociado> contatos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private int versao;

    /** Quem cadastrou; só existe até o INSERT, para o evento de criação. */
    @Transient
    private UUID cadastradoPor;

    protected Associado() {
    }

    /** CAR-02: a pessoa nasce com classificação padrão e o WhatsApp (e o e-mail, se houver) como principais. */
    public static Associado cadastrar(NovoAssociado novo, UUID autor) {
        var associado = new Associado();
        associado.documento = novo.documento();
        associado.aplicar(novo.dados());
        associado.perfil = Classificacao.PADRAO.perfil();
        associado.potencialIndicacao = Classificacao.PADRAO.potencial();
        associado.contatos().adicionar(new DadosContato(MeioContato.whatsapp(novo.whatsapp()), true, true));
        novo.email().ifPresent(email -> associado.contatos().adicionar(
                new DadosContato(MeioContato.email(email), true, true)));
        associado.cadastradoPor = Objects.requireNonNull(autor, "autor");
        return associado;
    }

    /** O id só existe depois do INSERT (identity): o evento de criação nasce aqui. */
    @PostPersist
    void registrarCadastro() {
        if (cadastradoPor != null) {
            registerEvent(new AssociadoCadastrado(id, nome, documento.valor(), municipioId,
                    contatos().resumo(), cadastradoPor));
            cadastradoPor = null;
        }
    }

    /** RG-06: recusa gravar sobre uma ficha que mudou desde que foi aberta. */
    public void conferirVersao(int versaoVista) {
        if (versaoVista != versao) {
            throw NegocioException.edicaoSimultanea();
        }
    }

    public void corrigirDados(DadosPessoais novos, UUID autor) {
        exigirNaoAnonimizado();
        var anteriores = dadosPessoais();
        if (!anteriores.equals(novos)) {
            aplicar(novos);
            registerEvent(new DadosPessoaisCorrigidos(id, anteriores, novos, autor));
        }
    }

    public void adicionarContato(DadosContato dados, UUID autor) {
        alterarContatos(contatos -> contatos.adicionar(dados), autor);
    }

    public void alterarContato(Long contatoId, DadosContato dados, UUID autor) {
        alterarContatos(contatos -> contatos.alterar(contatoId, dados), autor);
    }

    public void removerContato(Long contatoId, UUID autor) {
        alterarContatos(contatos -> contatos.remover(contatoId), autor);
    }

    /** CAR-06: qualquer papel interno classifica; a mudança vai para a auditoria. */
    public void classificar(Classificacao nova, UUID autor) {
        exigirNaoAnonimizado();
        var anterior = classificacao();
        if (!anterior.equals(nova)) {
            perfil = nova.perfil();
            potencialIndicacao = nova.potencial();
            registerEvent(new ClassificacaoAlterada(id, anterior, nova, autor));
        }
    }

    /**
     * CAR-08: apaga nome, documento, nascimento e contatos, de forma irreversível, e só quando todos os
     * contratos do associado estão cancelados (proposta). Os contratos e os atendimentos continuam, para os
     * relatórios; a cidade fica, para os indicadores por cidade.
     *
     * @param contratosDoAssociado todos os contratos da pessoa, para conferir a regra
     */
    public void anonimizar(List<Contrato> contratosDoAssociado, OffsetDateTime quando, UUID autor) {
        exigirNaoAnonimizado();
        if (contratosDoAssociado.stream().anyMatch(Contrato::vigente)) {
            throw NegocioException.invalido("contrato-vigente",
                    "Só é possível anonimizar quando todos os contratos do cliente estão cancelados.");
        }
        nome = PREFIXO_ANONIMIZADO + id;
        documento = null;
        dataNascimento = null;
        contatos().apagarTodos();
        anonimizadoEm = Objects.requireNonNull(quando);
        registerEvent(new AssociadoAnonimizado(id, usuarioId, autor));
    }

    public boolean anonimizado() {
        return anonimizadoEm != null;
    }

    public DadosPessoais dadosPessoais() {
        return new DadosPessoais(nome, dataNascimento, municipioId);
    }

    public Classificacao classificacao() {
        return new Classificacao(perfil, potencialIndicacao);
    }

    /** O WhatsApp usado nas filas e no botão de conversa (FIL-05). */
    public Optional<ContatoAssociado> whatsappPrincipal() {
        return contatos.stream().filter(c -> c.doTipo(TipoContatoAssociado.WHATSAPP) && c.isPrincipal()).findFirst();
    }

    private void alterarContatos(Consumer<ContatosDoAssociado> alteracao, UUID autor) {
        exigirNaoAnonimizado();
        var anteriores = contatos().resumo();
        alteracao.accept(contatos());
        registerEvent(new ContatosAlterados(id, anteriores, contatos().resumo(), autor));
    }

    private void aplicar(DadosPessoais dados) {
        if (dados.dataNascimento() != null && !documento.pessoaFisica()) {
            throw NegocioException.invalido("nascimento-pessoa-juridica", "Pessoa jurídica não tem data de nascimento.");
        }
        nome = dados.nome();
        dataNascimento = dados.dataNascimento();
        municipioId = dados.municipioId();
    }

    private void exigirNaoAnonimizado() {
        if (anonimizado()) {
            throw NegocioException.invalido("associado-anonimizado", "Este cadastro foi anonimizado e não pode ser alterado.");
        }
    }

    private ContatosDoAssociado contatos() {
        return new ContatosDoAssociado(contatos);
    }

    /** Eventos ainda não publicados; usado nos testes de domínio. */
    List<Object> eventosPendentes() {
        return List.copyOf(domainEvents());
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    /** Vazio só em cadastro anonimizado. */
    public Optional<Documento> getDocumento() {
        return Optional.ofNullable(documento);
    }

    public Optional<LocalDate> getDataNascimento() {
        return Optional.ofNullable(dataNascimento);
    }

    public Integer getMunicipioId() {
        return municipioId;
    }

    public Optional<UUID> getUsuarioId() {
        return Optional.ofNullable(usuarioId);
    }

    public Optional<OffsetDateTime> getAnonimizadoEm() {
        return Optional.ofNullable(anonimizadoEm);
    }

    public List<ContatoAssociado> getContatos() {
        return List.copyOf(contatos);
    }

    public int getVersao() {
        return versao;
    }
}
