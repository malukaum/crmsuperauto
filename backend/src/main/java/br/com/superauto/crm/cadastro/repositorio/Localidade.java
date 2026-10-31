package br.com.superauto.crm.cadastro.repositorio;

/** Cidade e UF do associado, vindas do município do IBGE (a UF nunca contradiz a cidade). */
public record Localidade(Integer municipioId, String cidade, String uf) {
}
