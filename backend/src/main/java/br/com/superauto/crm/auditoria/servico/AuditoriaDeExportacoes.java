package br.com.superauto.crm.auditoria.servico;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;
import br.com.superauto.crm.cadastro.entidade.CarteiraExportada;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** CAR-07 · Cada exportação fica na auditoria com o filtro usado e a quantidade de linhas. */
@Component
public class AuditoriaDeExportacoes {

    static final ItemAuditado CARTEIRA = new ItemAuditado("carteira", "exportacao");

    private final RegistroAuditoria auditoria;

    public AuditoriaDeExportacoes(RegistroAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoExportar(CarteiraExportada evento) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("filtro", evento.filtro());
        dados.put("linhas", evento.linhas());
        auditoria.registrar(EventoAuditoria.exportacao(CARTEIRA, evento.autor(), dados));
    }
}
