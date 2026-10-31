package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

/** ATD-03: repetir uma mensagem no mesmo ano exige confirmação explícita. */
public record MensagemDeAniversario(boolean jaRegistrada, boolean confirmarRepeticao) {
    public void exigirConfirmacao() {
        if (jaRegistrada && !confirmarRepeticao)
            throw NegocioException.conflito(
                    "aniversario-ja-registrado",
                    "Já há uma mensagem de aniversário registrada neste ano. Confirme para"
                            + " registrar outra.");
    }
}
