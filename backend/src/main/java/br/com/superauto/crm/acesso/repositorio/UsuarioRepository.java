package br.com.superauto.crm.acesso.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório da projeção local {@link Usuario}. */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** RG-06 · Carrega travando a linha até o fim da transação: alterações simultâneas entram em fila. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaAlteracao(UUID id);
}
