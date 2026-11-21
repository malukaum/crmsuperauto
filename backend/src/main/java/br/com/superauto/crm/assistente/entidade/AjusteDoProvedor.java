package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * IA-05 · O que o gerencial define para um provedor.
 *
 * @param versao a versão lida na tela (RG-06)
 * @param precoEntrada US$ por milhão de tokens de entrada; vazio = custo não estimado
 */
public record AjusteDoProvedor(
        int versao,
        String modelo,
        boolean ativo,
        BigDecimal limiteMensal,
        Optional<BigDecimal> precoEntrada,
        Optional<BigDecimal> precoSaida) {

    public AjusteDoProvedor {
        if (limiteMensal == null || limiteMensal.signum() < 0)
            throw NegocioException.invalido("limite-invalido", "O limite mensal não pode ser negativo.");
        if (precoEntrada.filter(p -> p.signum() < 0).isPresent() || precoSaida.filter(p -> p.signum() < 0).isPresent())
            throw NegocioException.invalido("preco-invalido", "O preço não pode ser negativo.");
        modelo = modelo == null || modelo.isBlank() ? null : modelo.strip();
    }
}
