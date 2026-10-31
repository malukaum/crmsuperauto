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
import java.util.Objects;
import java.util.UUID;

/**
 * Raiz do agregado Atendimento: um contato com o associado. É imutável (RG-07): a correção é um
 * novo registro (ATD-04). O registro pelas filas preserva o histórico e a anonimização pode apagar
 * descrições (CAR-08).
 */
@Entity
@Table(name = "atendimento")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atendimento extends AbstractAggregateRoot<Atendimento> {

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

    @Column(name = "realizado_em", nullable = false)
    private OffsetDateTime realizadoEm;

    @CreationTimestamp
    @Column(name = "registrado_em", nullable = false)
    private OffsetDateTime registradoEm;

    @Column(name = "registrado_por", nullable = false)
    private UUID registradoPor;

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
        if (novo.descricao() != null && novo.descricao().length() > 2000)
            throw NegocioException.invalido(
                    "resumo-longo", "O resumo deve ter até 2.000 caracteres.");
        Objects.requireNonNull(novo.associadoId(), "associadoId");
        Objects.requireNonNull(novo.tipoId(), "tipoId");
        Objects.requireNonNull(novo.resultado(), "resultado");
        Objects.requireNonNull(novo.agora(), "agora");
        Objects.requireNonNull(novo.autor(), "autor");
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

    @Transient private Novo registroPendente;

    @PostPersist
    void registrarCriacao() {
        if (registroPendente != null) {
            registerEvent(new AtendimentoRegistrado(id, registroPendente));
            registroPendente = null;
        }
    }

    /**
     * CAR-08 · A única mudança permitida num atendimento: a descrição pode conter dados pessoais e
     * é apagada quando o associado é anonimizado (proposta). Data, tipo, resultado e autor
     * continuam, para os relatórios.
     */
    public void apagarDescricaoPorAnonimizacao() {
        descricao = null;
    }
}
