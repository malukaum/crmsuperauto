package br.com.superauto.crm.acesso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Contratos vigentes (não cancelados) por vendedor, e a transferência deles quando o vendedor é desativado.
 * Contratos cancelados continuam com o vendedor original, como histórico.
 */
@Component
public class ContratosDoVendedor {

    private final NamedParameterJdbcTemplate jdbc;

    public ContratosDoVendedor(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<UUID, Long> contarPorVendedor() {
        Map<UUID, Long> contagem = new HashMap<>();
        jdbc.query("""
                SELECT vendedor_id, count(*) AS total
                  FROM contrato
                 WHERE vendedor_id IS NOT NULL AND situacao <> 'CANCELADO'
                 GROUP BY vendedor_id
                """, rs -> {
            contagem.put(rs.getObject("vendedor_id", UUID.class), rs.getLong("total"));
        });
        return contagem;
    }

    public long contar(UUID vendedor) {
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM contrato WHERE vendedor_id = :vendedor AND situacao <> 'CANCELADO'",
                new MapSqlParameterSource("vendedor", vendedor), Long.class);
        return total == null ? 0 : total;
    }

    /** Passa os contratos vigentes de um vendedor para outro, registrando cada troca na auditoria. */
    public int transferir(UUID de, UUID para, UUID autor) {
        return jdbc.update("""
                WITH alterados AS (
                    UPDATE contrato
                       SET vendedor_id = :para, atualizado_em = now(), versao = versao + 1
                     WHERE vendedor_id = :de AND situacao <> 'CANCELADO'
                 RETURNING id
                )
                INSERT INTO auditoria_evento (entidade, entidade_id, operacao, usuario_id, dados_anteriores, dados_novos)
                SELECT 'contrato', CAST(id AS text), 'ALTERACAO', :autor,
                       jsonb_build_object('vendedorId', CAST(:de AS text)),
                       jsonb_build_object('vendedorId', CAST(:para AS text))
                  FROM alterados
                """, new MapSqlParameterSource()
                .addValue("de", de)
                .addValue("para", para)
                .addValue("autor", autor));
    }
}
