package br.com.superauto.crm.auditoria;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/** Grava eventos em {@code auditoria_evento}: quem, quando, valor anterior e valor novo (RG-08). */
@Component
public class RegistroAuditoria {

    public static final String CRIACAO = "CRIACAO";
    public static final String ALTERACAO = "ALTERACAO";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public RegistroAuditoria(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void registrar(String entidade, String entidadeId, String operacao, UUID autor,
                          Map<String, ?> anterior, Map<String, ?> novo) {
        jdbc.update("""
                INSERT INTO auditoria_evento (entidade, entidade_id, operacao, usuario_id, dados_anteriores, dados_novos)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))
                """, entidade, entidadeId, operacao, autor, paraJson(anterior), paraJson(novo));
    }

    private String paraJson(Map<String, ?> dados) {
        return dados == null ? null : json.writeValueAsString(dados);
    }
}
