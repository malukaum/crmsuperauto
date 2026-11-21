package br.com.superauto.crm.auditoria.servico;

import br.com.superauto.crm.assistente.entidade.ConfiguracaoDeIaAlterada;
import br.com.superauto.crm.auditoria.entidade.Alteracao;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** RG-08 / IA-05 · Toda mudança na configuração do assistente de IA vai para a trilha, sem a chave. */
@Component
@RequiredArgsConstructor
public class AuditoriaDoAssistente {
    private final RegistroAuditoria auditoria;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAlterarConfiguracao(ConfiguracaoDeIaAlterada evento) {
        auditoria.registrar(EventoAuditoria.alteracao(new ItemAuditado(evento.item(), evento.id()), evento.autor(),
                new Alteracao(evento.anterior(), evento.novo())));
    }
}
