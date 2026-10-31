package br.com.superauto.crm.cadastro.servico;

import java.util.Objects;
import java.util.Set;

import br.com.superauto.crm.erro.NegocioException;

/**
 * Página da carteira pedida pela tela (CAR-01): páginas de 10, 25 ou 50 linhas.
 *
 * @param numero começa em zero
 */
public record PaginaSolicitada(int numero, int tamanho, Ordenacao ordenacao) {

    public static final Set<Integer> TAMANHOS = Set.of(10, 25, 50);

    public PaginaSolicitada {
        Objects.requireNonNull(ordenacao, "ordenacao");
        if (numero < 0 || !TAMANHOS.contains(tamanho)) {
            throw NegocioException.invalido("pagina-invalida", "Escolha páginas de 10, 25 ou 50 linhas.");
        }
    }
}
