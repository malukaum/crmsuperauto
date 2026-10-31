package br.com.superauto.crm.auditoria.servico;

import static br.com.superauto.crm.auditoria.servico.Campos.campo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.auditoria.entidade.Alteracao;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;
import br.com.superauto.crm.cadastro.entidade.CodigoSgaAlterado;
import br.com.superauto.crm.cadastro.entidade.ContratoCadastrado;
import br.com.superauto.crm.cadastro.entidade.ContratoTransferido;
import br.com.superauto.crm.cadastro.entidade.MensalidadeReajustada;
import br.com.superauto.crm.cadastro.entidade.SituacaoContratoAlterada;

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
    private static final String CAMPO_SITUACAO = "situacao";
    private static final String CAMPO_MENSALIDADE = "mensalidade";
    private static final String CAMPO_CODIGO_SGA = "codigoSga";

    private final RegistroAuditoria auditoria;

    public AuditoriaDeContratos(RegistroAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoCadastrar(ContratoCadastrado evento) {
        Map<String, Object> novo = new LinkedHashMap<>();
        novo.put("associadoId", evento.associadoId());
        novo.put("veiculoId", evento.veiculoId());
        novo.put(CAMPO_VENDEDOR, evento.vendedorId().toString());
        novo.put(CAMPO_MENSALIDADE, evento.mensalidade().valor().toPlainString());
        novo.put("dataAdesao", evento.dataAdesao().toString());
        auditoria.registrar(EventoAuditoria.criacao(item(evento.contratoId()), evento.autor(), novo));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoTransferir(ContratoTransferido evento) {
        alterou(evento.contratoId(), evento.autor(),
                campo(CAMPO_VENDEDOR, evento.vendedorAnterior()), campo(CAMPO_VENDEDOR, evento.novoVendedor()));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAlterarSituacao(SituacaoContratoAlterada evento) {
        alterou(evento.contratoId(), evento.autor(),
                campo(CAMPO_SITUACAO, evento.anterior()), campo(CAMPO_SITUACAO, evento.nova()));
    }

    /** CAR-05: o contrato guarda só o valor atual; os dois valores ficam aqui. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoReajustar(MensalidadeReajustada evento) {
        alterou(evento.contratoId(), evento.autor(),
                campo(CAMPO_MENSALIDADE, evento.anterior().valor().toPlainString()),
                campo(CAMPO_MENSALIDADE, evento.nova().valor().toPlainString()));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAlterarCodigoSga(CodigoSgaAlterado evento) {
        alterou(evento.contratoId(), evento.autor(),
                campo(CAMPO_CODIGO_SGA, evento.anterior()), campo(CAMPO_CODIGO_SGA, evento.novo()));
    }

    private void alterou(Long contratoId, UUID autor, Map<String, ?> anterior, Map<String, ?> novo) {
        auditoria.registrar(EventoAuditoria.alteracao(item(contratoId), autor, new Alteracao(anterior, novo)));
    }

    private static ItemAuditado item(Long contratoId) {
        return ItemAuditado.de(ENTIDADE, contratoId);
    }
}
