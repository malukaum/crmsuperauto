package br.com.superauto.crm.cadastro.entidade;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Um WhatsApp, telefone ou e-mail do associado. Parte interna do agregado {@link Associado}: não tem
 * repositório e só muda pelos métodos da raiz, que garantem um principal por tipo e nenhum repetido.
 */
@Entity
@Table(name = "contato_associado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContatoAssociado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private MeioContato meio;

    @Column(nullable = false)
    private boolean principal;

    @Column(name = "aceita_contato", nullable = false)
    private boolean aceitaContato;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    ContatoAssociado(DadosContato dados) {
        aplicar(dados);
    }

    void aplicar(DadosContato dados) {
        this.meio = dados.meio();
        this.principal = dados.principal();
        this.aceitaContato = dados.aceitaContato();
    }

    void tornarPrincipal() {
        principal = true;
    }

    void deixarDeSerPrincipal() {
        principal = false;
    }

    boolean mesmoMeio(MeioContato outro) {
        return meio.equals(outro);
    }

    boolean doTipo(TipoContatoAssociado tipo) {
        return meio.tipo() == tipo;
    }

    ContatoResumo resumo() {
        return new ContatoResumo(meio.tipo(), meio.valor(), principal, aceitaContato);
    }
}
