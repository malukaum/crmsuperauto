package br.com.superauto.crm.referencia.repositorio;

import java.util.List;
import java.util.Optional;

import br.com.superauto.crm.referencia.entidade.Municipio;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Só leitura: os municípios vêm do IBGE pelo Liquibase. */
public interface MunicipioRepository extends Repository<Municipio, Integer> {

    Optional<Municipio> findById(Integer id);

    /** Busca por trecho do nome, sem diferença de acento nem de maiúsculas (RG-05), em ordem alfabética. */
    @Query("""
            select m from Municipio m
             where texto_busca(m.nome) like concat('%', texto_busca(:trecho), '%')
             order by texto_busca(m.nome), m.ufSigla
            """)
    List<Municipio> buscarPorNome(String trecho, Limit limite);
}
