package br.com.superauto.crm.acesso;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Quantidades exibidas ao lado de "Contatos de hoje" e "Atrasados" no menu (ACE-02).
 * Vêm da visão das filas, que já exclui contratos suspensos e cancelados e usa o "hoje" de São Paulo.
 * O vendedor só conta os contratos em que é o vendedor (RG-03).
 */
@Service
public class ContadoresMenuService {

    private final NamedParameterJdbcTemplate jdbc;

    public ContadoresMenuService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Contadores(long contatosHoje, long atrasados) {
    }

    /** @param vendedor preenchido só quando quem pergunta é vendedor (escopo da carteira dele) */
    public Contadores contar(Optional<UUID> vendedor) {
        var params = new MapSqlParameterSource("vendedor", vendedor.orElse(null));
        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE fila = 'HOJE')     AS hoje,
                       count(*) FILTER (WHERE fila = 'ATRASADO') AS atrasados
                  FROM vw_fila_contato
                 WHERE CAST(:vendedor AS uuid) IS NULL OR vendedor_id = CAST(:vendedor AS uuid)
                """, params, (rs, i) -> new Contadores(rs.getLong("hoje"), rs.getLong("atrasados")));
    }
}
