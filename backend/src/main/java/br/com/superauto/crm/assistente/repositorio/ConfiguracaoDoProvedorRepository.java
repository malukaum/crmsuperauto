package br.com.superauto.crm.assistente.repositorio;

import br.com.superauto.crm.assistente.entidade.ConfiguracaoDoProvedor;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Repositório do agregado {@link ConfiguracaoDoProvedor}. */
public interface ConfiguracaoDoProvedorRepository extends JpaRepository<ConfiguracaoDoProvedor, ProvedorDeIa> {

    /** RG-06 · Carrega travando a linha até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracaoDoProvedor c where c.provedor = :provedor")
    Optional<ConfiguracaoDoProvedor> buscarParaAlteracao(ProvedorDeIa provedor);
}
