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

    private static final int DIAS_ATE_NOVA_TENTATIVA = 2;

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

    /** ATD-01 · Aplica o resultado do contato na régua; retorno manual sempre prevalece (RG-14). */
    public void registrarContato(RegistroNaRegua registro, ContextoDoContato contexto, UUID autor) {
        alterarAgenda(autor, () -> aplicar(registro, contexto));
    }

    /** ATD-01 · O que o registro faria com a régua, sem alterar este acompanhamento. */
    public EstadoDoAcompanhamento prever(RegistroNaRegua registro, ContextoDoContato contexto) {
        var previsao = new AcompanhamentoContrato();
        previsao.contratoId = contratoId;
        previsao.etapaPendenteId = etapaPendenteId;
        previsao.retornoManualEm = retornoManualEm;
        previsao.proximoContatoEm = proximoContatoEm;
        previsao.versao = versao;
        previsao.aplicar(registro, contexto);
        return previsao.estado();
    }

    /**
     * ATD-04 · O gerencial corrige a régua depois de um atendimento lançado errado: define a etapa pendente e, se
     * quiser, um retorno manual. Sem retorno, o próximo contato volta a ser a base da régua + o prazo da etapa.
     */
    public void ajustarPelaGerencia(AjusteDaRegua ajuste, ContextoDoAjuste contexto, UUID autor) {
        exigirAlteravel(ajuste.versao());
        exigirRetornoAPartirDe(ajuste.retorno(), contexto.hoje());
        EtapaRegua etapa = EtapaRegua.daRegua(contexto.etapas(), ajuste.etapaId());
        alterarAgenda(autor, () -> {
            etapaPendenteId = etapa.id();
            retornoManualEm = ajuste.retorno().orElse(null);
            proximoContatoEm = ajuste.retorno().orElse(contexto.baseRegua().plusDays(etapa.diasAposAnterior()));
        });
    }

    /** FIL-06 · Recalcula o próximo contato com os novos prazos; o retorno manual é mantido. */
    public void recalcularAgenda(LocalDate base, List<EtapaRegua> etapas, UUID autor) {
        alterarAgenda(autor, () -> proximoContatoEm = preverRecalculo(base, etapas).orElse(null));
    }

    /** FIL-06 · O próximo contato depois do recálculo, para a prévia contar quem muda de data. */
    public Optional<LocalDate> preverRecalculo(LocalDate base, List<EtapaRegua> etapas) {
        if (!reguaAtiva() || retornoManualEm != null) return getProximoContatoEm();
        return Optional.of(base.plusDays(EtapaRegua.daRegua(etapas, etapaPendenteId).diasAposAnterior()));
    }

    /** A etapa que o contato trabalha (a pendente); régua encerrada não recebe mais contato. */
    public Short etapaEmTrabalho() {
        if (etapaPendenteId == null) throw NegocioException.invalido("regua-encerrada", "A régua está encerrada.");
        return etapaPendenteId;
    }

    /** A régua ainda tem etapa pendente (não está encerrada). */
    public boolean reguaAtiva() {
        return etapaPendenteId != null;
    }

    /** FIL-06 · Se o recálculo com os novos prazos mudaria a data do próximo contato. */
    public boolean mudariaDeDataNoRecalculo(LocalDate base, List<EtapaRegua> etapas) {
        return !getProximoContatoEm().equals(preverRecalculo(base, etapas));
    }

    public EstadoDoAcompanhamento estado() {
        return new EstadoDoAcompanhamento(
                getEtapaPendenteId(), getProximoContatoEm(), getRetornoManualEm());
    }

    /** RG-08 · Toda mudança da agenda passa por aqui: se a agenda mudou, publica o anterior e o novo. */
    private void alterarAgenda(UUID autor, Runnable mudanca) {
        var anterior = estado();
        mudanca.run();
        if (!anterior.equals(estado()))
            registerEvent(new AcompanhamentoAtualizado(contratoId, autor, anterior, estado()));
    }

    private void aplicar(RegistroNaRegua registro, ContextoDoContato contexto) {
        Objects.requireNonNull(registro.resultado(), "resultado");
        exigirAlteravel(contexto.versao());
        exigirRetornoAPartirDe(registro.retorno(), contexto.hoje());
        if (registro.resultado() == ResultadoContato.REAGENDADO && registro.retorno().isEmpty())
            throw NegocioException.invalido("retorno-obrigatorio", "Informe a data combinada para o retorno.");
        if (registro.resultado() == ResultadoContato.CONCLUIDO) concluirEtapa(contexto);
        if (registro.resultado() == ResultadoContato.SEM_RESPOSTA)
            agendarRetorno(contexto.hoje().plusDays(DIAS_ATE_NOVA_TENTATIVA));
        registro.retorno().ifPresent(this::agendarRetorno);
    }

    /** RG-06: quem viu outra versão não altera; régua encerrada não muda mais. */
    private void exigirAlteravel(int versaoVista) {
        if (versao != versaoVista) throw NegocioException.edicaoSimultanea();
        etapaEmTrabalho();
    }

    private static void exigirRetornoAPartirDe(Optional<LocalDate> retorno, LocalDate hoje) {
        if (retorno.filter(data -> data.isBefore(hoje)).isPresent())
            throw NegocioException.invalido("retorno-invalido", "O retorno não pode ser anterior a hoje.");
    }

    /** O próximo contato conta a partir do dia do atendimento, que pode ser anterior a hoje (ATD-01). */
    private void concluirEtapa(ContextoDoContato contexto) {
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
}
