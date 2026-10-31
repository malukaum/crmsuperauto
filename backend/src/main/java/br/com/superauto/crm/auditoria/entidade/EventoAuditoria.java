package br.com.superauto.crm.auditoria.entidade;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Um evento de auditoria (RG-08): quem, quando, valor anterior e valor novo. Só é inserido, nunca alterado
 * ({@link Immutable}). Os mapas viram {@code jsonb} pelo próprio Hibernate.
 */
@Entity
@Immutable
@Table(name = "auditoria_evento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String entidade;

    @Column(name = "entidade_id", nullable = false, length = 40)
    private String entidadeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperacaoAuditoria operacao;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @CreationTimestamp
    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private OffsetDateTime ocorridoEm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_anteriores", columnDefinition = "jsonb")
    private Map<String, Object> dadosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_novos", columnDefinition = "jsonb")
    private Map<String, Object> dadosNovos;

    private EventoAuditoria(ItemAuditado item, OperacaoAuditoria operacao, UUID autor, Alteracao alteracao) {
        this.entidade = item.entidade();
        this.entidadeId = item.id();
        this.operacao = operacao;
        this.usuarioId = autor;
        this.dadosAnteriores = copia(alteracao.anterior());
        this.dadosNovos = copia(alteracao.novo());
    }

    /** O item nasceu com os dados informados. */
    public static EventoAuditoria criacao(ItemAuditado item, UUID autor, Map<String, ?> dados) {
        return new EventoAuditoria(item, OperacaoAuditoria.CRIACAO, autor, new Alteracao(null, dados));
    }

    /** O item mudou: valores anteriores e novos só dos campos alterados. */
    public static EventoAuditoria alteracao(ItemAuditado item, UUID autor, Alteracao alteracao) {
        return new EventoAuditoria(item, OperacaoAuditoria.ALTERACAO, autor, alteracao);
    }

    /** Alguém viu um dado pessoal protegido, como o documento completo (RG-04). */
    public static EventoAuditoria visualizacao(ItemAuditado item, UUID autor, Map<String, ?> dados) {
        return new EventoAuditoria(item, OperacaoAuditoria.VISUALIZACAO, autor, new Alteracao(null, dados));
    }

    /** Alguém exportou dados (CAR-07): o filtro usado e a quantidade de linhas. */
    public static EventoAuditoria exportacao(ItemAuditado item, UUID autor, Map<String, ?> dados) {
        return new EventoAuditoria(item, OperacaoAuditoria.EXPORTACAO, autor, new Alteracao(null, dados));
    }

    /** Os dados pessoais foram apagados (CAR-08). Nada pessoal é guardado no evento. */
    public static EventoAuditoria anonimizacao(ItemAuditado item, UUID autor) {
        return new EventoAuditoria(item, OperacaoAuditoria.ANONIMIZACAO, autor, new Alteracao(null, null));
    }

    private static Map<String, Object> copia(Map<String, ?> dados) {
        return dados == null ? null : new LinkedHashMap<>(dados);
    }
}
