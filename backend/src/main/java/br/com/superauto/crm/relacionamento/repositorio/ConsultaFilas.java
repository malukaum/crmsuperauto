package br.com.superauto.crm.relacionamento.repositorio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDeAniversarios;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas filtradas na persistência; toda seleção recebe o escopo do vendedor (RG-03). */
public interface ConsultaFilas {
    record Contatos(
            boolean atrasados, LocalDate hoje, FiltroDaFila filtro, Optional<UUID> vendedor) {}

    /** Uma página da fila, na ordem da fila (a ordenação pedida pelo cliente é ignorada). */
    Page<ContratoNaFila> contatos(Contatos consulta, Pageable pagina);

    long contarContatos(Contatos consulta);

    List<ContratoNaFila> aniversarios(PeriodoDeAniversarios periodo, Optional<UUID> vendedor);
}
