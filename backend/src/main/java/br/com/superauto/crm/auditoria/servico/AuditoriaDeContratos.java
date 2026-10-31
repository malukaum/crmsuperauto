package br.com.superauto.crm.auditoria.servico;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.auditoria.entidade.Alteracao;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;
import br.com.superauto.crm.cadastro.entidade.ContratoTransferido;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Registra na trilha de auditoria (RG-08) os eventos de domínio do contexto Cadastro sobre contratos.
 * Roda antes do commit, na mesma transação da alteração: se a auditoria falhar, a alteração é desfeita.
 */
@Component
public class AuditoriaDeContratos {

    static final String ENTIDADE = "contrato";
    private static final String CAMPO_VENDEDOR = "vendedorId";

    private final RegistroAuditoria auditoria;

    public AuditoriaDeContratos(RegistroAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoTransferir(ContratoTransferido evento) {
        var alteracao = new Alteracao(vendedor(evento.vendedorAnterior()), vendedor(evento.novoVendedor()));
        auditoria.registrar(EventoAuditoria.alteracao(
                ItemAuditado.de(ENTIDADE, evento.contratoId()), evento.autor(), alteracao));
    }

    private static Map<String, Object> vendedor(UUID vendedorId) {
        Map<String, Object> dados = new HashMap<>();
        dados.put(CAMPO_VENDEDOR, vendedorId == null ? null : vendedorId.toString());
        return dados;
    }
}
