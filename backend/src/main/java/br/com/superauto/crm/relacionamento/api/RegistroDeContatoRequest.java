package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.servico.OrigemDoRegistro;
import br.com.superauto.crm.relacionamento.servico.RegistroDeContato;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/** ATD-01 · Corpo do registro de pós-venda, igual pela fila e pela ficha. */
public record RegistroDeContatoRequest(
        @NotNull(message = "Informe a versão do acompanhamento.")
                @Min(value = 0, message = "A versão não pode ser negativa.")
                Integer versao,
        @NotNull(message = "Informe o resultado do contato.") ResultadoContato resultado,
        LocalDate retorno,
        @Size(max = 2000, message = "O resumo deve ter até 2.000 caracteres.") String descricao,
        LocalDateTime realizadoEm) {

    RegistroDeContato comando(OrigemDoRegistro origem) {
        return new RegistroDeContato(
                versao,
                resultado,
                Optional.ofNullable(retorno),
                descricao,
                Optional.ofNullable(realizadoEm),
                origem);
    }
}
