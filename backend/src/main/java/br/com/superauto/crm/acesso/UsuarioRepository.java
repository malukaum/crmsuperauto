package br.com.superauto.crm.acesso;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Cria a projeção ou atualiza username/nome/e-mail se mudaram no Keycloak. Não mexe em {@code ativo}:
     * quem ativa e desativa é a gestão de usuários.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO usuario (id, username, nome, email)
            VALUES (:id, :username, :nome, :email)
            ON CONFLICT (id) DO UPDATE
               SET username = EXCLUDED.username,
                   nome = EXCLUDED.nome,
                   email = EXCLUDED.email,
                   atualizado_em = now()
             WHERE (usuario.username, usuario.nome, usuario.email)
                   IS DISTINCT FROM (EXCLUDED.username, EXCLUDED.nome, EXCLUDED.email)
            """, nativeQuery = true)
    int registrar(@Param("id") UUID id, @Param("username") String username,
                  @Param("nome") String nome, @Param("email") String email);

    @Modifying
    @Transactional
    @Query(value = "UPDATE usuario SET ativo = :ativo, atualizado_em = now() WHERE id = :id", nativeQuery = true)
    int definirAtivo(@Param("id") UUID id, @Param("ativo") boolean ativo);
}
