package br.com.superauto.crm.relacionamento.repositorio;

/** Filtros opcionais da fila; os valores nulos representam parâmetros HTTP não informados. */
public record FiltroDaFila(String busca, Integer cidade, Short etapa, Short tipoVeiculo) {
    public static final FiltroDaFila SEM_FILTROS = new FiltroDaFila(null, null, null, null);
}
