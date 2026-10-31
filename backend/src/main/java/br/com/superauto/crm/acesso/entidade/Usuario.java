package br.com.superauto.crm.acesso.entidade;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.compartilhado.entidade.EmailConverter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * Projeção local de quem acessa pelo Keycloak. Os papéis ficam só no Keycloak; aqui guardamos
 * o necessário para as chaves estrangeiras (autor de atendimento, vendedor do contrato) e para exibir nomes.
 * O usuário nunca é excluído, só desativado, para o histórico manter o autor.
 *
 * <p>O id vem do Keycloak (não é gerado), por isso implementa {@link Persistable}: assim o
 * {@code save} de um usuário novo faz um INSERT direto, sem o SELECT prévio do {@code merge}.
 */
@Entity
@Table(name = "usuario")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario implements Persistable<UUID> {

    /** O usuário técnico que assina o que veio da planilha. Não existe no Keycloak. */
    public static final UUID SISTEMA_IMPORTACAO = new UUID(0L, 0L);

    @Id
    private UUID id;

    @Getter
    @Column(nullable = false, length = 100)
    private String username;

    @Getter
    @Column(nullable = false, length = 200)
    private String nome;

    @Convert(converter = EmailConverter.class)
    @Column(length = Email.TAMANHO_MAXIMO)
    private Email email;

    @Getter
    @Column(nullable = false)
    private boolean ativo;

    @Getter
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Getter
    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Transient
    private boolean novo;

    public Usuario(UUID id, String username, String nome, Email email) {
        this.id = Objects.requireNonNull(id);
        this.username = username;
        this.nome = nome;
        this.email = email;
        this.ativo = true;
        this.novo = true;
    }

    /**
     * Atualiza username, nome e e-mail vindos do Keycloak. Não mexe em {@code ativo}: quem ativa e
     * desativa é a gestão de usuários.
     *
     * @return se algo mudou (o Hibernate só emite o UPDATE nesse caso)
     */
    public boolean atualizarDados(String username, String nome, Email email) {
        if (Objects.equals(this.username, username) && Objects.equals(this.nome, nome)
                && Objects.equals(this.email, email)) {
            return false;
        }
        this.username = username;
        this.nome = nome;
        this.email = email;
        return true;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @PostLoad
    @PostPersist
    void marcarPersistido() {
        this.novo = false;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Optional<Email> getEmail() {
        return Optional.ofNullable(email);
    }
}
