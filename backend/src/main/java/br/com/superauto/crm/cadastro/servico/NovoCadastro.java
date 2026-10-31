package br.com.superauto.crm.cadastro.servico;

import java.util.Objects;
import java.util.Optional;

import br.com.superauto.crm.compartilhado.entidade.Documento;

/**
 * CAR-02 · Comando de cadastro de contrato. O cadastro começa pelo documento: se a pessoa já existe, só o veículo
 * e o contrato são usados; senão, {@code pessoa} é obrigatória.
 */
public record NovoCadastro(Documento documento, Optional<NovaPessoa> pessoa, NovoVeiculo veiculo,
                           CondicoesContrato contrato) {

    public NovoCadastro {
        Objects.requireNonNull(documento, "documento");
        Objects.requireNonNull(pessoa, "pessoa");
        Objects.requireNonNull(veiculo, "veiculo");
        Objects.requireNonNull(contrato, "contrato");
    }
}
