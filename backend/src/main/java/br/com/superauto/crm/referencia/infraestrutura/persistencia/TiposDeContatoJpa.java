package br.com.superauto.crm.referencia.infraestrutura.persistencia;

import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TiposDeContatoJpa implements TiposDeContato {
    private final EntityManager em;

    public Short id(TipoDeRelacionamento tipo) {
        return em.createQuery(
                        "select t.id from TipoContato t where t.codigo = :codigo", Short.class)
                .setParameter("codigo", tipo.name())
                .getSingleResult();
    }
}
