package br.com.superauto.crm.relacionamento.repositorio;

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

    List<ContratoNaFila> contatos(Contatos consulta, int limite);

    long contarContatos(Contatos consulta);

    List<ContratoNaFila> aniversarios(PeriodoDeAniversarios periodo, Optional<UUID> vendedor);
}
