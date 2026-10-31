package br.com.superauto.crm.cadastro.repositorio;

import java.util.Optional;

import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.compartilhado.entidade.Documento;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do agregado {@link Associado}. */
public interface AssociadoRepository extends JpaRepository<Associado, Long> {

    /** Uma pessoa por documento; anonimizados não têm documento e nunca são encontrados. */
    Optional<Associado> findByDocumento(Documento documento);
}
