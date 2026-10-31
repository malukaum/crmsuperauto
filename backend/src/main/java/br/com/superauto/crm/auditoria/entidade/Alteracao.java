package br.com.superauto.crm.auditoria.entidade;

import java.util.Map;

/** Valores anteriores e novos dos campos que mudaram. Mapas podem conter valores nulos. */
public record Alteracao(Map<String, ?> anterior, Map<String, ?> novo) {
}
