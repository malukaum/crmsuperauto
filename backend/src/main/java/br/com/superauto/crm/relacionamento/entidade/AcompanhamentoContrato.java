package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Raiz do agregado AcompanhamentoContrato: onde o contrato está na régua de pós-venda (1:1 com o
 * contrato, que é outro agregado e é referenciado só pelo id). Muda pelo trabalho da equipe, não
 * pelo SGA, por isso fica separado.
 *
 * <p>O próximo contato é derivado (RG-14) e gravado para indexar as filas. O id é o do contrato
 * (atribuído), por isso implementa {@link Persistable}: o {@code save} de um acompanhamento novo
 * faz um INSERT direto.
 */
@Entity
@Table(name = "acompanhamento_contrato")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcompanhamentoContrato extends AbstractAggregateRoot<AcompanhamentoContrato>
        implements Persistable<Long> {

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

    @Getter
    @Version
    @Column(nullable = false)
    private int versao;

    @Transient private boolean novo;

    /**
     * CAR-02 · O contrato novo entra na régua na etapa inicial (D+7) pendente, com o próximo
     * contato em adesão + prazo da etapa inicial (RG-14): adesão em 13/03/2026 → 20/03/2026.
     *
     * @param regua as etapas da régua; exatamente uma é a inicial
     */
    public static AcompanhamentoContrato iniciar(
            Long contratoId, List<EtapaRegua> regua, LocalDate dataAdesao) {
        EtapaRegua inicial =
                regua.stream()
                        .filter(EtapaRegua::inicial)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "A régua de pós-venda não tem etapa inicial."));
        var acompanhamento = new AcompanhamentoContrato();
        acompanhamento.contratoId = Objects.requireNonNull(contratoId, "contratoId");
        acompanhamento.etapaPendenteId = inicial.id();
        acompanhamento.proximoContatoEm = dataAdesao.plusDays(inicial.diasAposAnterior());
        acompanhamento.novo = true;
        return acompanhamento;
    }

    private static final int DIAS_ATE_NOVA_TENTATIVA = 2;

    /** Aplica o resultado na régua; retorno manual sempre prevalece (RG-14). */
    public void registrar(
            ResultadoContato resultado, Optional<LocalDate> retorno, ContextoContato contexto) {
        validarRegistro(resultado, retorno, contexto);
        if (resultado == ResultadoContato.CONCLUIDO) concluirEtapa(contexto);
        if (resultado == ResultadoContato.SEM_RESPOSTA)
            agendarRetorno(contexto.hoje().plusDays(DIAS_ATE_NOVA_TENTATIVA));
        retorno.ifPresent(this::agendarRetorno);
        marcarAlterado(contexto.realizadoEm());
    }

    /**
     * Todo registro avança a versão, mesmo o Pendente, que não muda a régua: assim um registro simultâneo do mesmo
     * contrato é recusado (RG-06). O valor gravado vem do {@code @UpdateTimestamp}.
     */
    private void marcarAlterado(OffsetDateTime momento) {
        atualizadoEm = momento;
    }

    private void validarRegistro(
            ResultadoContato resultado, Optional<LocalDate> retorno, ContextoContato contexto) {
        Objects.requireNonNull(resultado, "resultado");
        if (versao != contexto.versao()) throw NegocioException.edicaoSimultanea();
        if (etapaPendenteId == null)
            throw NegocioException.invalido("regua-encerrada", "A régua está encerrada.");
        if (retorno.filter(data -> data.isBefore(contexto.hoje())).isPresent()) {
            throw NegocioException.invalido(
                    "retorno-invalido", "O retorno não pode ser anterior a hoje.");
        }
        if (resultado == ResultadoContato.REAGENDADO && retorno.isEmpty()) {
            throw NegocioException.invalido(
                    "retorno-obrigatorio", "Informe a data combinada para o retorno.");
        }
    }

    /** O próximo contato conta a partir do dia do atendimento, que pode ser anterior a hoje (ATD-01). */
    private void concluirEtapa(ContextoContato contexto) {
        EtapaRegua atual = contexto.etapa(etapaPendenteId);
        EtapaRegua proxima = contexto.etapa(atual.proximaEtapaId());
        etapaPendenteId = proxima.id();
        retornoManualEm = null;
        proximoContatoEm = contexto.dataDoAtendimento().plusDays(proxima.diasAposAnterior());
    }

    private void agendarRetorno(LocalDate data) {
        retornoManualEm = data;
        proximoContatoEm = data;
    }

    public AcompanhamentoContrato prever(
            ResultadoContato resultado, Optional<LocalDate> retorno, ContextoContato contexto) {
        var previsao = new AcompanhamentoContrato();
        previsao.contratoId = contratoId;
        previsao.etapaPendenteId = etapaPendenteId;
        previsao.retornoManualEm = retornoManualEm;
        previsao.proximoContatoEm = proximoContatoEm;
        previsao.versao = versao;
        previsao.registrar(resultado, retorno, contexto);
        return previsao;
    }

    /** @param realizadoEm quando o atendimento aconteceu, no fuso de São Paulo */
    public record ContextoContato(
            int versao, LocalDate hoje, OffsetDateTime realizadoEm, List<EtapaRegua> etapas) {
        public ContextoContato {
            etapas = List.copyOf(etapas);
        }

        LocalDate dataDoAtendimento() {
            return realizadoEm.toLocalDate();
        }

        EtapaRegua etapa(Short id) {
            return etapaDa(etapas, id);
        }
    }

    /** @param baseRegua o dia do último contato concluído ou, sem nenhum, da adesão (RG-14) */
    public record ContextoDoAjuste(LocalDate hoje, LocalDate baseRegua, List<EtapaRegua> etapas) {
        public ContextoDoAjuste {
            Objects.requireNonNull(hoje, "hoje");
            Objects.requireNonNull(baseRegua, "baseRegua");
            etapas = List.copyOf(etapas);
        }
    }

    private static EtapaRegua etapaDa(List<EtapaRegua> etapas, Short id) {
        return etapas.stream()
                .filter(etapa -> etapa.id().equals(id))
                .findFirst()
                .orElseThrow(
                        () ->
                                NegocioException.invalido(
                                        "etapa-inexistente", "Etapa não encontrada na régua."));
    }

    /**
     * ATD-04 · O gerencial corrige a régua depois de um atendimento lançado errado: define a etapa pendente e, se
     * quiser, um retorno manual. Sem retorno, o próximo contato volta a ser a base da régua + o prazo da etapa.
     */
    public void ajustarPelaGerencia(AjusteDaRegua ajuste, ContextoDoAjuste contexto, UUID autor) {
        if (versao != ajuste.versao()) throw NegocioException.edicaoSimultanea();
        if (etapaPendenteId == null)
            throw NegocioException.invalido("regua-encerrada", "A régua está encerrada.");
        if (ajuste.retorno().filter(data -> data.isBefore(contexto.hoje())).isPresent())
            throw NegocioException.invalido(
                    "retorno-invalido", "O retorno não pode ser anterior a hoje.");
        var anterior = estado();
        EtapaRegua etapa = etapaDa(contexto.etapas(), ajuste.etapaId());
        etapaPendenteId = etapa.id();
        retornoManualEm = ajuste.retorno().orElse(null);
        proximoContatoEm =
                ajuste.retorno().orElse(contexto.baseRegua().plusDays(etapa.diasAposAnterior()));
        registerEvent(new AcompanhamentoAtualizado(contratoId, autor, anterior, estado()));
    }

    public Optional<LocalDate> preverRecalculo(LocalDate base, List<EtapaRegua> etapas) {
        if (etapaPendenteId == null || retornoManualEm != null) return getProximoContatoEm();
        return Optional.of(base.plusDays(etapaDa(etapas, etapaPendenteId).diasAposAnterior()));
    }

    public void recalcular(LocalDate base, List<EtapaRegua> etapas) {
        proximoContatoEm = preverRecalculo(base, etapas).orElse(null);
    }

    public EstadoDoAcompanhamento estado() {
        return new EstadoDoAcompanhamento(
                getEtapaPendenteId(), getProximoContatoEm(), getRetornoManualEm());
    }

    public void registrarContato(RegistroNaRegua registro, ContextoContato contexto, UUID autor) {
        var anterior = estado();
        registrar(registro.resultado(), registro.retorno(), contexto);
        registerEvent(new AcompanhamentoAtualizado(contratoId, autor, anterior, estado()));
    }

    public void recalcularAgenda(LocalDate base, List<EtapaRegua> etapas, UUID autor) {
        var anterior = estado();
        recalcular(base, etapas);
        if (!anterior.equals(estado()))
            registerEvent(new AcompanhamentoAtualizado(contratoId, autor, anterior, estado()));
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
}
