package br.com.superauto.crm.acesso.repositorio;

import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
}
