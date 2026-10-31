package br.com.superauto.crm.cadastro.entidade;

import java.time.LocalDate;
import java.util.Objects;

import br.com.superauto.crm.erro.NegocioException;

/**
 * Os dados da pessoa que a equipe corrige na ficha: nome, nascimento (só pessoa física) e cidade.
 *
 * @param dataNascimento nulo quando não informado
 */
public record DadosPessoais(String nome, LocalDate dataNascimento, Integer municipioId) {

    public static final int TAMANHO_MAXIMO_NOME = 200;

    public DadosPessoais {
        nome = Objects.requireNonNullElse(nome, "").strip().replaceAll("\\s+", " ");
        if (nome.isEmpty() || nome.length() > TAMANHO_MAXIMO_NOME) {
            throw NegocioException.invalido("nome-invalido", "Informe o nome, com até 200 caracteres.");
        }
        if (municipioId == null) {
            throw NegocioException.invalido("cidade-obrigatoria", "Informe a cidade e a UF.");
        }
    }
}
