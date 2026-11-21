package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * ATD-01 · Comando de registro de um contato de pós-venda.
 *
 * @param versao a versão do acompanhamento lida na fila ou na ficha (RG-06)
 * @param retorno retorno manual, que prevalece sobre o cálculo da régua
 * @param realizadoEm vazio = agora; senão, até 7 dias atrás (proposta)
 */
public record RegistroDeContato(
        int versao,
        ResultadoContato resultado,
        Optional<LocalDate> retorno,
        String descricao,
        Optional<LocalDateTime> realizadoEm,
        OrigemDoRegistro origem) {

    public RegistroDeContato {
        Objects.requireNonNull(retorno, "retorno");
        Objects.requireNonNull(realizadoEm, "realizadoEm");
        Objects.requireNonNull(origem, "origem");
    }
}
