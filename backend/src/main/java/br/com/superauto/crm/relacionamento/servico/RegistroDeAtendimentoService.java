package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.ContextoDoContato;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.entidade.MensagemDeAniversario;
import br.com.superauto.crm.relacionamento.entidade.MomentoDoAtendimento;
import br.com.superauto.crm.relacionamento.entidade.RegistroNaRegua;
import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;

/**
 * ATD-01 e ATD-03 (pelas filas FIL-02 a FIL-04 e pela ficha): grava histórico e régua juntos; as raízes publicam os
 * eventos de domínio para a auditoria.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RegistroDeAtendimentoService {
    private final CarteiraVisivel carteira;
    private final AcompanhamentoContratoRepository acompanhamentos;
    private final AtendimentoRepository atendimentos;
    private final ReguaDePosVenda regua;
    private final TiposDeContato tipos;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public PreviaDoContato prever(Long contratoId, RegistroDeContato registro, AutorDoContato autor) {
        var contrato = exigirDisponivel(contratoId, registro, autor);
        var contexto = contexto(registro);
        var previsao = exigirAcompanhamento(contratoId)
                .prever(new RegistroNaRegua(registro.resultado(), registro.retorno()), contexto);
        String proxima = EtapaRegua.daRegua(contexto.etapas(), previsao.etapa().orElseThrow()).nome();
        return new PreviaDoContato(
                registro.resultado() == ResultadoContato.CONCLUIDO ? contrato.getEtapa() : null,
                proxima,
                previsao.proximoContato().orElse(null));
    }

    public void registrar(Long contratoId, RegistroDeContato registro, AutorDoContato autor) {
        var contrato = exigirDisponivel(contratoId, registro, autor);
        var acompanhamento =
                acompanhamentos
                        .buscarParaAlteracao(contratoId)
                        .orElseThrow(() -> NegocioException.naoEncontrado("Acompanhamento não encontrado."));
        var contexto = contexto(registro);
        Short etapa = acompanhamento.etapaEmTrabalho();
        acompanhamento.registrarContato(
                new RegistroNaRegua(registro.resultado(), registro.retorno()), contexto, autor.id());
        atendimentos.save(
                Atendimento.registrar(
                        new Atendimento.Novo(
                                contrato.getAssociadoId(),
                                contratoId,
                                tipos.id(TipoDeRelacionamento.POS_VENDA),
                                etapa,
                                registro.resultado(),
                                registro.descricao(),
                                registro.retorno().orElse(null),
                                contexto.realizadoEm(),
                                autor.id())));
        acompanhamentos.save(acompanhamento);
    }

    public void aniversario(Long contratoId, AutorDoContato autor) {
        var contrato = carteira.contrato(contratoId, autor.vendedor());
        contrato.exigirAniversarioDisponivel();
        var agora = ZonedDateTime.now(relogio);
        boolean jaRegistrada =
                atendimentos.existsByAssociadoIdAndAnoAniversario(contrato.getAssociadoId(), agora.getYear());
        atendimentos.save(
                Atendimento.registrarMensagemDeAniversario(
                        new MensagemDeAniversario(
                                contrato.getAssociadoId(),
                                tipos.id(TipoDeRelacionamento.ANIVERSARIO),
                                agora.toOffsetDateTime(),
                                autor.id(),
                                jaRegistrada)));
    }

    private ContextoDoContato contexto(RegistroDeContato registro) {
        var agora = ZonedDateTime.now(relogio);
        var momento = MomentoDoAtendimento.de(registro.realizadoEm(), agora);
        return new ContextoDoContato(
                registro.versao(), agora.toLocalDate(), momento.valor(), regua.etapas());
    }

    private AcompanhamentoContrato exigirAcompanhamento(Long id) {
        return acompanhamentos
                .findById(id)
                .orElseThrow(() -> NegocioException.naoEncontrado("Acompanhamento não encontrado."));
    }

    private ContratoNaFila exigirDisponivel(Long id, RegistroDeContato registro, AutorDoContato autor) {
        var contrato = carteira.contrato(id, autor.vendedor());
        registro.origem().exigirDisponivel(contrato);
        return contrato;
    }
}
