package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Raiz do agregado Atendimento: um contato com o associado. É imutável (RG-07): a correção é um novo registro que
 * aponta para o original (ATD-04). A anonimização pode apagar descrições (CAR-08).
 */
@Entity
@Table(name = "atendimento")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atendimento extends AbstractAggregateRoot<Atendimento> {
    private static final int TAMANHO_MAXIMO_DO_RESUMO = 2000;

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "associado_id", nullable = false, updatable = false)
    private Long associadoId;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "contrato_id")
    private Long contratoId;

    @Column(name = "tipo_contato_id", nullable = false)
    private Short tipoContatoId;

    @Column(name = "etapa_id")
    private Short etapaId;

    @Enumerated(EnumType.STRING)
    private ResultadoContato resultado;

    @Column(name = "retorno_agendado_para")
    private LocalDate retornoAgendadoPara;

    @Column(name = "corrige_atendimento_id", updatable = false)
    private Long corrigeAtendimentoId;

    /** ATD-03: preenchido só na mensagem de aniversário (não nas correções), para uma por pessoa no ano. */
    @Column(name = "ano_aniversario", updatable = false)
    private Integer anoAniversario;

    @Column(name = "realizado_em", nullable = false)
    private OffsetDateTime realizadoEm;

    @CreationTimestamp
    @Column(name = "registrado_em", nullable = false)
    private OffsetDateTime registradoEm;

    @Column(name = "registrado_por", nullable = false)
    private UUID registradoPor;

    @Transient private Novo registroPendente;

    public record Novo(
            Long associadoId,
            Long contratoId,
            Short tipoId,
            Short etapaId,
            ResultadoContato resultado,
            String descricao,
            LocalDate retorno,
            OffsetDateTime agora,
            UUID autor) {}

    public static Atendimento registrar(Novo novo) {
        validar(novo);
        var atendimento = new Atendimento();
        atendimento.registroPendente = novo;
        atendimento.associadoId = novo.associadoId();
        atendimento.contratoId = novo.contratoId();
        atendimento.tipoContatoId = novo.tipoId();
        atendimento.etapaId = novo.etapaId();
        atendimento.resultado = novo.resultado();
        atendimento.descricao = novo.descricao();
        atendimento.retornoAgendadoPara = novo.retorno();
        atendimento.realizadoEm = novo.agora();
        atendimento.registradoPor = novo.autor();
        return atendimento;
    }

    /** ATD-03 · A mensagem de aniversário: tipo Aniversário, Concluído, ligada à pessoa e sem mexer na régua. */
    public static Atendimento registrarMensagemDeAniversario(MensagemDeAniversario mensagem) {
        mensagem.exigirPrimeiraDoAno();
        var atendimento = registrar(new Novo(mensagem.associadoId(), null, mensagem.tipoId(), null,
                ResultadoContato.CONCLUIDO, null, null, mensagem.agora(), mensagem.autor()));
        atendimento.anoAniversario = mensagem.ano();
        return atendimento;
    }

    private static void validar(Novo novo) {
        Objects.requireNonNull(novo.associadoId(), "associadoId");
        Objects.requireNonNull(novo.tipoId(), "tipoId");
        Objects.requireNonNull(novo.resultado(), "resultado");
        Objects.requireNonNull(novo.agora(), "agora");
        Objects.requireNonNull(novo.autor(), "autor");
        if (novo.descricao() != null && novo.descricao().length() > TAMANHO_MAXIMO_DO_RESUMO)
            throw NegocioException.invalido("resumo-longo", "O resumo deve ter até 2.000 caracteres.");
        if (novo.resultado() == ResultadoContato.REAGENDADO && novo.retorno() == null)
            throw NegocioException.invalido("retorno-obrigatorio", "Informe a data combinada para o retorno.");
    }

    /**
     * ATD-04 · Gera o registro de correção, que aponta para este. Contrato, tipo, etapa e data do contato continuam
     * os do original; a régua não é desfeita. O autor corrige até o fim do dia seguinte ao registro; o gerencial, a
     * qualquer momento.
     *
     * @param agora o instante atual no fuso de São Paulo (RG-01)
     */
    public Atendimento corrigir(Correcao correcao, ResponsavelPelaCorrecao responsavel, ZonedDateTime agora) {
        new PermissaoDeCorrecao(registradoPor, registradoEm).exigir(responsavel, agora);
        var correcaoRegistrada =
                registrar(
                        new Novo(
                                associadoId,
                                contratoId,
                                tipoContatoId,
                                etapaId,
                                correcao.resultado(),
                                correcao.descricao(),
                                correcao.retorno().orElse(null),
                                realizadoEm,
                                responsavel.id()));
        correcaoRegistrada.corrigeAtendimentoId = id;
        return correcaoRegistrada;
    }

    /**
     * ATD-04 · Um atendimento tem no máximo uma correção; para corrigir de novo, corrige-se a correção (também
     * garantido pelo índice único {@code uk_atendimento_corrige}).
     */
    public void exigirSemCorrecao(boolean existeCorrecao) {
        if (existeCorrecao)
            throw NegocioException.conflito(
                    "atendimento-ja-corrigido", "Este atendimento já foi corrigido. Corrija o registro de correção.");
    }

    @PostPersist
    void registrarCriacao() {
        if (registroPendente != null) {
            registerEvent(
                    new AtendimentoRegistrado(id, registroPendente, Optional.ofNullable(corrigeAtendimentoId)));
            registroPendente = null;
        }
    }

    /**
     * CAR-08 · A única mudança permitida num atendimento: a descrição pode conter dados pessoais e é apagada quando o
     * associado é anonimizado. Data, tipo, resultado e autor continuam, para os relatórios.
     */
    public void apagarDescricaoPorAnonimizacao() {
        descricao = null;
    }

    public Optional<Long> getContratoId() {
        return Optional.ofNullable(contratoId);
    }

    public Optional<Long> getCorrigeAtendimentoId() {
        return Optional.ofNullable(corrigeAtendimentoId);
    }
}
