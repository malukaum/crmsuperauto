package br.com.superauto.crm.auditoria.servico;

import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.repositorio.EventoAuditoriaRepository;

import org.springframework.stereotype.Component;

/** Grava eventos em {@code auditoria_evento}: quem, quando, valor anterior e valor novo (RG-08). */
@Component
public class RegistroAuditoria {

    private final EventoAuditoriaRepository eventos;

    public RegistroAuditoria(EventoAuditoriaRepository eventos) {
        this.eventos = eventos;
    }

    public void registrar(EventoAuditoria evento) {
        eventos.save(evento);
    }
}
