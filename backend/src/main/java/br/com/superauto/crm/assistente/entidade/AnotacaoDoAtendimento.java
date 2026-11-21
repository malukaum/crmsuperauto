package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * IA-01 / IA-02 · Raiz do agregado: as anotações livres de um atendimento, os anexos e o resumo estruturado gerado
 * pela IA. O id é o do atendimento (outro agregado, referenciado só pelo id). O resumo nunca muda régua, contrato nem
 * classificação: é informação para a equipe e para os indicadores.
 */
@Entity
@Table(name = "anotacao_atendimento")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnotacaoDoAtendimento implements Persistable<Long> {
    /** Depois disso o resumo é dado como impossível e o material é descartado. */
    static final int MAXIMO_DE_TENTATIVAS = 5;
    private static final List<Duration> ESPERA_ENTRE_TENTATIVAS =
            List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(60));
    private static final int TAMANHO_DO_ERRO = 500;

    @Getter
    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @Getter
    @Column(name = "associado_id")
    private Long associadoId;

    private String texto;

    @Getter
    @Enumerated(EnumType.STRING)
    private SituacaoDoResumo situacao;

    @Getter
    private int tentativas;

    @Column(name = "proxima_tentativa_em")
    private OffsetDateTime proximaTentativaEm;

    @Column(name = "resumo_curto")
    private String resumoCurto;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> assuntos;

    @Enumerated(EnumType.STRING)
    private Satisfacao satisfacao;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> objecoes;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> interesses;

    @Column(name = "risco_cancelamento")
    private Boolean riscoCancelamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "chance_indicar")
    private ChanceDeIndicar chanceIndicar;

    @Column(name = "proxima_acao")
    private String proximaAcao;

    @Enumerated(EnumType.STRING)
    private ProvedorDeIa provedor;

    private String modelo;

    @Column(name = "resumido_em")
    private OffsetDateTime resumidoEm;

    private String erro;

    @Getter
    @Column(name = "registrado_por")
    private UUID registradoPor;

    @Getter
    @CreationTimestamp
    @Column(name = "registrado_em")
    private OffsetDateTime registradoEm;

    @Version
    private int versao;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "atendimento_id", nullable = false)
    private List<AnexoDoAtendimento> anexos = new ArrayList<>();

    @Transient private boolean nova;

    public static AnotacaoDoAtendimento registrar(NovaAnotacao nova, UUID autor) {
        var anotacao = new AnotacaoDoAtendimento();
        anotacao.atendimentoId = nova.atendimentoId();
        anotacao.associadoId = nova.associadoId();
        anotacao.texto = nova.texto();
        anotacao.registradoPor = Objects.requireNonNull(autor, "autor");
        anotacao.situacao = SituacaoDoResumo.AGUARDANDO;
        nova.anexos().forEach(anexo -> anotacao.anexos.add(AnexoDoAtendimento.de(anexo)));
        anotacao.nova = true;
        return anotacao;
    }

    /** IA-02 · Guarda o resumo gerado e descarta o conteúdo dos anexos, que não é mais necessário. */
    public void registrarResumo(ResumoDoAtendimento resumo, ProvedorDeIa quem, String modeloUsado, OffsetDateTime agora) {
        if (situacao != SituacaoDoResumo.AGUARDANDO)
            throw NegocioException.conflito("resumo-ja-concluido", "Esta anotação já foi resumida.");
        resumoCurto = resumo.resumoCurto();
        assuntos = resumo.assuntos().stream().sorted(Comparator.naturalOrder()).map(Enum::name).toList();
        satisfacao = resumo.satisfacao();
        objecoes = resumo.objecoes();
        interesses = resumo.interesses();
        riscoCancelamento = resumo.riscoDeCancelamento();
        chanceIndicar = resumo.chanceDeIndicar();
        proximaAcao = resumo.proximaAcao();
        provedor = quem;
        modelo = modeloUsado;
        resumidoEm = agora;
        erro = null;
        proximaTentativaEm = null;
        situacao = SituacaoDoResumo.RESUMIDO;
        descartarAnexos(agora);
    }

    /**
     * IA-01 · O resumo não saiu: tenta de novo mais tarde, esperando cada vez mais. Na quinta falha desiste e
     * descarta os anexos (LGPD: não guardar o que não vai ser usado).
     */
    public void registrarFalha(String motivo, OffsetDateTime agora) {
        tentativas++;
        erro = motivo == null ? null : motivo.substring(0, Math.min(motivo.length(), TAMANHO_DO_ERRO));
        if (tentativas >= MAXIMO_DE_TENTATIVAS) {
            situacao = SituacaoDoResumo.FALHOU;
            proximaTentativaEm = null;
            descartarAnexos(agora);
        } else {
            proximaTentativaEm = agora.plus(ESPERA_ENTRE_TENTATIVAS.get(tentativas - 1));
        }
    }

    /** IA-01 · O assistente está desligado ou sem provedor: espera sem gastar tentativa. */
    public void adiarSemTentativa(OffsetDateTime ate) {
        proximaTentativaEm = ate;
    }

    /** IA-06 / CAR-08 · Apaga o que identifica o cliente; os sinais não identificáveis continuam nos indicadores. */
    public void anonimizar(OffsetDateTime agora) {
        texto = null;
        resumoCurto = null;
        objecoes = List.of();
        interesses = List.of();
        proximaAcao = null;
        erro = null;
        descartarAnexos(agora);
        anexos.clear();
    }

    private void descartarAnexos(OffsetDateTime agora) {
        anexos.forEach(anexo -> anexo.descartarConteudo(agora));
    }

    public Optional<ResumoDoAtendimento> resumo() {
        if (situacao != SituacaoDoResumo.RESUMIDO || resumoCurto == null) return Optional.empty();
        var conjunto = EnumSet.noneOf(Assunto.class);
        assuntos.forEach(nome -> conjunto.add(Assunto.valueOf(nome)));
        return Optional.of(new ResumoDoAtendimento(resumoCurto, conjunto, satisfacao, objecoes, interesses,
                Boolean.TRUE.equals(riscoCancelamento), chanceIndicar, proximaAcao));
    }

    public Optional<String> getTexto() {
        return Optional.ofNullable(texto);
    }

    public List<AnexoDoAtendimento> getAnexos() {
        return List.copyOf(anexos);
    }

    public Optional<ProvedorDeIa> getProvedor() {
        return Optional.ofNullable(provedor);
    }

    public Optional<String> getModelo() {
        return Optional.ofNullable(modelo);
    }

    public Optional<OffsetDateTime> getResumidoEm() {
        return Optional.ofNullable(resumidoEm);
    }

    public Optional<String> getErro() {
        return Optional.ofNullable(erro);
    }

    @PostLoad
    @PostPersist
    void marcarPersistida() {
        nova = false;
    }

    @Override
    public Long getId() {
        return atendimentoId;
    }

    @Override
    public boolean isNew() {
        return nova;
    }
}
