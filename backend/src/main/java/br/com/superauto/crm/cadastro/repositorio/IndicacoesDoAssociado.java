package br.com.superauto.crm.cadastro.repositorio;

import java.util.List;

/**
 * As indicações feitas por um associado, da mais recente à mais antiga (CAR-03). Porta declarada pelo Cadastro e
 * implementada pelo Comercial, dono das indicações.
 */
public interface IndicacoesDoAssociado {

    List<IndicacaoFeita> feitasPor(Long associadoId);
}
