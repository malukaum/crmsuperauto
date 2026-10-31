package br.com.superauto.crm.acesso;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Projeção local de quem acessa pelo Keycloak. Os papéis ficam só no Keycloak; aqui guardamos
 * o necessário para as chaves estrangeiras (autor de atendimento, vendedor do contrato) e para exibir nomes.
 * O usuário nunca é excluído, só desativado, para o histórico manter o autor.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    /** O usuário técnico que assina o que veio da planilha. Não existe no Keycloak. */
    public static final UUID SISTEMA_IMPORTACAO = new UUID(0L, 0L);

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(length = 254)
    private String email;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false, insertable = false)
    private OffsetDateTime atualizadoEm;

    protected Usuario() {
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
