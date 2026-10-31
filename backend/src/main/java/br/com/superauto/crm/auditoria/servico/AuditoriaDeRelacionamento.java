package br.com.superauto.crm.auditoria.servico;

import br.com.superauto.crm.auditoria.entidade.Alteracao;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;
import br.com.superauto.crm.referencia.entidade.PrazoDaEtapaAlterado;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoAtualizado;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.AtendimentoRegistrado;
import br.com.superauto.crm.relacionamento.entidade.EstadoDoAcompanhamento;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/** RG-08: traduz eventos de negócio em auditoria, na transação que alterou o dado. */
@Component
@RequiredArgsConstructor
public class AuditoriaDeRelacionamento {
    private final RegistroAuditoria auditoria;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAtualizarAgenda(AcompanhamentoAtualizado evento) {
        auditoria.registrar(
                EventoAuditoria.alteracao(
                        ItemAuditado.de("acompanhamento_contrato", evento.contratoId()),
                        evento.autor(),
                        new Alteracao(agenda(evento.anterior()), agenda(evento.novo()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAlterarPrazo(PrazoDaEtapaAlterado evento) {
        auditoria.registrar(
                EventoAuditoria.alteracao(
                        ItemAuditado.de("etapa_relacionamento", evento.etapaId().longValue()),
                        evento.autor(),
                        new Alteracao(
                                Map.of("diasAposAnterior", evento.anterior()),
                                Map.of("diasAposAnterior", evento.novo()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoRegistrarAtendimento(AtendimentoRegistrado evento) {
        auditoria.registrar(
                EventoAuditoria.criacao(
                        ItemAuditado.de("atendimento", evento.atendimentoId()),
                        evento.dados().autor(),
                        atendimento(evento.dados())));
    }

    private static Map<String, Object> agenda(EstadoDoAcompanhamento estado) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("etapa", estado.etapa().orElse(null));
        dados.put("proximoContato", estado.proximoContato().map(Object::toString).orElse(null));
        dados.put("retornoManual", estado.retornoManual().map(Object::toString).orElse(null));
        return dados;
    }

    private static Map<String, Object> atendimento(Atendimento.Novo contato) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("associadoId", contato.associadoId());
        dados.put("contratoId", contato.contratoId());
        dados.put("tipoContatoId", contato.tipoId());
        dados.put("etapaId", contato.etapaId());
        dados.put("resultado", contato.resultado().name());
        dados.put("descricao", contato.descricao());
        dados.put(
                "retornoAgendadoPara",
                contato.retorno() == null ? null : contato.retorno().toString());
        dados.put("realizadoEm", contato.agora().toString());
        dados.put("registradoPor", contato.autor().toString());
        return dados;
    }
}
