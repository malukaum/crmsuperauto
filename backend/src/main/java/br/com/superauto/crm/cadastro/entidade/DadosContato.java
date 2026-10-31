package br.com.superauto.crm.cadastro.entidade;

import java.util.Objects;

/**
 * Um contato como a equipe o informa (CAR-04).
 *
 * @param aceitaContato falso = opt-out: o número continua visível, mas não é oferecido para mensagem (RG-11)
 */
public record DadosContato(MeioContato meio, boolean principal, boolean aceitaContato) {

    public DadosContato {
        Objects.requireNonNull(meio, "meio");
    }
}
