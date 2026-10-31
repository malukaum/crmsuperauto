package br.com.superauto.crm.referencia.entidade;

import br.com.superauto.crm.erro.NegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.UUID;

/**
 * Uma etapa da régua de pós-venda (RG-13): D+7 → D+30 → D+60 → D+90 → Trimestral, que aponta para
 * si mesma. O prazo conta a partir do contato anterior concluído, ou da adesão na etapa inicial.
 * Prazos configuráveis pelo gerencial (FIL-06).
 */
@Entity
@Table(name = "etapa_relacionamento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtapaRelacionamento extends AbstractAggregateRoot<EtapaRelacionamento> {

    @Version private int versao;

    @Id private Short id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String nome;

    @Column(nullable = false)
    private short ordem;

    @Column(name = "dias_apos_anterior", nullable = false)
    private short diasAposAnterior;

    @Column(name = "proxima_etapa_id")
    private Short proximaEtapaId;

    @Column(nullable = false)
    private boolean inicial;

    public void alterarPrazo(int dias, UUID autor) {
        if (dias < 1 || dias > 365) {
            throw NegocioException.invalido("prazo-invalido", "Cada etapa aceita de 1 a 365 dias.");
        }
        if (diasAposAnterior != dias) {
            registerEvent(new PrazoDaEtapaAlterado(id, diasAposAnterior, dias, autor));
            diasAposAnterior = (short) dias;
        }
    }
}
