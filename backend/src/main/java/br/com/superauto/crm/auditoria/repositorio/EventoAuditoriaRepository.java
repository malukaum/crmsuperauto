package br.com.superauto.crm.auditoria.repositorio;

import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
}
