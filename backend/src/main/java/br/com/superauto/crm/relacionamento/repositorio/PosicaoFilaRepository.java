package br.com.superauto.crm.relacionamento.repositorio;

import java.util.UUID;

import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.relacionamento.entidade.PosicaoFila;

import org.springframework.data.repository.Repository;

/** Só leitura: a visão não aceita escrita, então nem expõe save/delete. */
public interface PosicaoFilaRepository extends Repository<PosicaoFila, Long> {

    long countByFila(Fila fila);

    long countByFilaAndVendedorId(Fila fila, UUID vendedorId);
}
