package br.com.superauto.crm.auditoria.servico;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.PapelAlterado;
import br.com.superauto.crm.acesso.entidade.UsuarioCriado;
import br.com.superauto.crm.acesso.entidade.UsuarioDesativado;
import br.com.superauto.crm.acesso.entidade.UsuarioReativado;
import br.com.superauto.crm.auditoria.entidade.Alteracao;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Registra na trilha de auditoria (RG-08) os eventos de domínio do contexto Acesso sobre usuários da equipe.
 * Dentro de uma transação, roda antes do commit (se a operação for desfeita, o registro também é); fora de
 * transação, roda na hora ({@code fallbackExecution}), porque parte da gestão de usuários acontece só no Keycloak.
 */
@Component
public class AuditoriaDeUsuarios {

    static final String ENTIDADE = "usuario";
    private static final String CAMPO_ATIVO = "ativo";
    private static final String CAMPO_PAPEL = "papel";

    private final RegistroAuditoria auditoria;

    public AuditoriaDeUsuarios(RegistroAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void aoCriar(UsuarioCriado evento) {
        Map<String, Object> novo = new LinkedHashMap<>();
        novo.put("username", evento.username());
        novo.put("nome", evento.nome());
        novo.put("email", evento.email() == null ? null : evento.email().valor());
        novo.put(CAMPO_PAPEL, evento.papel());
        novo.put(CAMPO_ATIVO, true);
        auditoria.registrar(EventoAuditoria.criacao(item(evento.usuarioId()), evento.autor(), novo));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void aoAlterarPapel(PapelAlterado evento) {
        alterou(evento.usuarioId(), evento.autor(),
                new Alteracao(Map.of(CAMPO_PAPEL, evento.papelAnterior()), Map.of(CAMPO_PAPEL, evento.papelNovo())));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void aoDesativar(UsuarioDesativado evento) {
        Map<String, Object> novo = new LinkedHashMap<>();
        novo.put(CAMPO_ATIVO, false);
        if (evento.contratosTransferidos() > 0) {
            novo.put("contratosTransferidos", evento.contratosTransferidos());
            novo.put("transferidosPara", evento.transferidosPara().toString());
        }
        alterou(evento.usuarioId(), evento.autor(), new Alteracao(Map.of(CAMPO_ATIVO, evento.estavaAtivo()), novo));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void aoReativar(UsuarioReativado evento) {
        alterou(evento.usuarioId(), evento.autor(),
                new Alteracao(Map.of(CAMPO_ATIVO, evento.estavaAtivo()), Map.of(CAMPO_ATIVO, true)));
    }

    private void alterou(UUID usuarioId, UUID autor, Alteracao alteracao) {
        auditoria.registrar(EventoAuditoria.alteracao(item(usuarioId), autor, alteracao));
    }

    private static ItemAuditado item(UUID usuarioId) {
        return ItemAuditado.de(ENTIDADE, usuarioId);
    }
}
