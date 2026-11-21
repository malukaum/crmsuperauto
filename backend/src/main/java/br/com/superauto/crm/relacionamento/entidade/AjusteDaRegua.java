package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * ATD-04 · O que o gerencial define ao ajustar a régua de um contrato depois de um atendimento lançado errado.
 *
 * @param versao a versão do acompanhamento lida na ficha (RG-06)
 * @param retorno vazio = o próximo contato volta a ser calculado pela etapa
 */
public record AjusteDaRegua(int versao, Short etapaId, Optional<LocalDate> retorno) {

    public AjusteDaRegua {
        Objects.requireNonNull(etapaId, "etapaId");
        Objects.requireNonNull(retorno, "retorno");
    }
}
