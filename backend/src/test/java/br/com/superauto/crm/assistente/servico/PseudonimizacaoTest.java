package br.com.superauto.crm.assistente.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.superauto.crm.assistente.entidade.DadosIdentificaveis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PseudonimizacaoTest {
    private final Pseudonimizacao pseudonimizacao = new Pseudonimizacao();

    @Test
    @DisplayName("IA-06: nome, placa e telefone saem do texto antes do envio e voltam na resposta, no servidor")
    void criterioDeAceite() {
        var texto = pseudonimizacao.pseudonimizar("José da Silva, placa HAA7216, fone 37 99869-2950",
                new DadosIdentificaveis("José da Silva"));

        assertThat(texto.texto()).doesNotContain("José", "Silva", "HAA7216", "99869").contains("[CLIENTE]", "[PLACA_1]", "[TELEFONE_1]");
        assertThat(pseudonimizacao.restaurar("Ligar para [CLIENTE] sobre [PLACA_1]", texto.marcadores()))
                .isEqualTo("Ligar para José da Silva sobre HAA7216");
    }

    @Test
    @DisplayName("IA-06: parte do nome, sem acento, também sai; CPF e e-mail de qualquer pessoa também")
    void outrosDados() {
        var texto = pseudonimizacao.pseudonimizar("O jose mandou o CPF 529.982.247-25 e o e-mail jose@exemplo.com",
                new DadosIdentificaveis("José da Silva"));

        assertThat(texto.texto()).doesNotContain("jose", "529.982.247-25", "@exemplo.com")
                .contains("[CLIENTE]", "[DOCUMENTO_1]", "[EMAIL_1]");
    }
}
