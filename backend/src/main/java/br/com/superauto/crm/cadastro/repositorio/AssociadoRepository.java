package br.com.superauto.crm.cadastro.repositorio;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import br.com.superauto.crm.cadastro.entidade.Associado;
import br.com.superauto.crm.compartilhado.entidade.Documento;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do agregado {@link Associado}. */
public interface AssociadoRepository extends JpaRepository<Associado, Long> {

    /** Uma pessoa por documento; anonimizados não têm documento e nunca são encontrados. */
    Optional<Associado> findByDocumento(Documento documento);

    /** RG-06 · Carrega travando a linha até o fim da transação: alterações simultâneas entram em fila. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Associado a where a.id = :id")
    Optional<Associado> buscarParaAlteracao(Long id);
}
