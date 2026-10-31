package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * Raiz do agregado AcompanhamentoContrato: onde o contrato está na régua de pós-venda (1:1 com o contrato, que é
 * outro agregado e é referenciado só pelo id). Muda pelo trabalho da equipe, não pelo SGA, por isso fica separado.
 *
 * <p>O próximo contato é derivado (RG-14) e gravado para indexar as filas. O id é o do contrato (atribuído),
 * por isso implementa {@link Persistable}: o {@code save} de um acompanhamento novo faz um INSERT direto.
 */
@Entity
@Table(name = "acompanhamento_contrato")
public class AcompanhamentoContrato implements Persistable<Long> {

    @Id
    @Column(name = "contrato_id")
    private Long contratoId;

    /** Vazio = régua encerrada. */
    @Column(name = "etapa_pendente_id")
    private Short etapaPendenteId;

    @Column(name = "retorno_manual_em")
    private LocalDate retornoManualEm;

    @Column(name = "proximo_contato_em")
    private LocalDate proximoContatoEm;

    @Column(name = "encerrado_em")
    private OffsetDateTime encerradoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private int versao;

    @Transient
    private boolean novo;

    protected AcompanhamentoContrato() {
    }

    /**
     * CAR-02 · O contrato novo entra na régua na etapa inicial (D+7) pendente, com o próximo contato em
     * adesão + prazo da etapa inicial (RG-14): adesão em 13/03/2026 → 20/03/2026.
     *
     * @param regua as etapas da régua; exatamente uma é a inicial
     */
    public static AcompanhamentoContrato iniciar(Long contratoId, List<EtapaRegua> regua, LocalDate dataAdesao) {
        EtapaRegua inicial = regua.stream().filter(EtapaRegua::inicial).findFirst()
                .orElseThrow(() -> new IllegalStateException("A régua de pós-venda não tem etapa inicial."));
        var acompanhamento = new AcompanhamentoContrato();
        acompanhamento.contratoId = Objects.requireNonNull(contratoId, "contratoId");
        acompanhamento.etapaPendenteId = inicial.id();
        acompanhamento.proximoContatoEm = dataAdesao.plusDays(inicial.diasAposAnterior());
        acompanhamento.novo = true;
        return acompanhamento;
    }

    @PostLoad
    @PostPersist
    void marcarPersistido() {
        novo = false;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    @Override
    public Long getId() {
        return contratoId;
    }

    public Optional<Short> getEtapaPendenteId() {
        return Optional.ofNullable(etapaPendenteId);
    }

    public Optional<LocalDate> getRetornoManualEm() {
        return Optional.ofNullable(retornoManualEm);
    }

    public Optional<LocalDate> getProximoContatoEm() {
        return Optional.ofNullable(proximoContatoEm);
    }

    public Optional<OffsetDateTime> getEncerradoEm() {
        return Optional.ofNullable(encerradoEm);
    }

    public int getVersao() {
        return versao;
    }
}
