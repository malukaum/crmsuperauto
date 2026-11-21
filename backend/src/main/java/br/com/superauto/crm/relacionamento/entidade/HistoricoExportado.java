package br.com.superauto.crm.relacionamento.entidade;

import java.util.Map;
import java.util.UUID;

/** Evento de domínio: alguém exportou o histórico de atendimentos (ATD-05, regras da CAR-07). */
public record HistoricoExportado(Map<String, Object> filtro, int linhas, UUID autor) {

    public HistoricoExportado {
        filtro = Map.copyOf(filtro);
    }
}
