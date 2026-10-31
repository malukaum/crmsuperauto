package br.com.superauto.crm.referencia.entidade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

@Immutable
@Entity
@Table(name = "tipo_contato")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TipoContato {
    @Id private Short id;
    private String codigo;
}
