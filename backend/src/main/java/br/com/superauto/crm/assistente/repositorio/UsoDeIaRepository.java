package br.com.superauto.crm.assistente.repositorio;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.UsoDeIa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Registro das chamadas à IA: só inserção e somas. */
public interface UsoDeIaRepository extends Repository<UsoDeIa, Long> {

    UsoDeIa save(UsoDeIa uso);

    /** IA-05 · Quanto o provedor já custou no período (só chamadas com preço conhecido). */
    @Query("""
            select coalesce(sum(u.custo), 0) from UsoDeIa u
             where u.provedor = :provedor and u.realizadoEm >= :inicio and u.realizadoEm < :fim
            """)
    BigDecimal gastoNoPeriodo(ProvedorDeIa provedor, OffsetDateTime inicio, OffsetDateTime fim);

    @Query("""
            select new br.com.superauto.crm.assistente.repositorio.UsoPorProvedor(
                   u.provedor, count(u),
                   sum(case when u.sucesso = false then 1 else 0 end),
                   sum(case when u.reserva = true then 1 else 0 end),
                   coalesce(sum(u.custo), 0))
              from UsoDeIa u
             where u.realizadoEm >= :inicio and u.realizadoEm < :fim
             group by u.provedor
            """)
    List<UsoPorProvedor> usoNoPeriodo(OffsetDateTime inicio, OffsetDateTime fim);
}
