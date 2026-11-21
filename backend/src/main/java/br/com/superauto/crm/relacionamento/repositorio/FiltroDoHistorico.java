package br.com.superauto.crm.relacionamento.repositorio;

import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * ATD-05 · Filtros opcionais do histórico, além do período: tipo, resultado, usuário, cidade e busca por cliente ou
 * placa (RG-05).
 */
public record FiltroDoHistorico(
        Optional<TipoDeRelacionamento> tipo,
        Optional<ResultadoContato> resultado,
        Optional<UUID> usuario,
        Optional<Integer> cidade,
        Optional<String> busca) {

    public FiltroDoHistorico {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(cidade, "cidade");
        busca = Objects.requireNonNull(busca, "busca").map(String::strip).filter(termo -> !termo.isEmpty());
    }
}
