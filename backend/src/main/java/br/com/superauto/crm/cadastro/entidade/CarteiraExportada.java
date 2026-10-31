package br.com.superauto.crm.cadastro.entidade;

import java.util.Map;
import java.util.UUID;

/** Evento de domínio: alguém exportou a carteira (CAR-07), com o filtro usado e a quantidade de linhas. */
public record CarteiraExportada(Map<String, Object> filtro, int linhas, UUID autor) {

    public CarteiraExportada {
        filtro = Map.copyOf(filtro);
    }
}
