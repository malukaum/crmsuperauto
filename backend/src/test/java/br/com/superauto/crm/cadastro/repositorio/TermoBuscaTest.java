package br.com.superauto.crm.cadastro.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermoBuscaTest {

    @Test
    @DisplayName("RG-05: \"haa-7216\" vira HAA7216 para casar com a placa")
    void placa() {
        assertThat(new TermoBusca("haa-7216").alfanumerico()).contains("HAA7216");
    }

    @Test
    @DisplayName("RG-05: \"103.034\" vira 103034 para casar com o documento e o telefone")
    void documento() {
        var termo = new TermoBusca("103.034");

        assertThat(termo.alfanumerico()).contains("103034");
        assertThat(termo.digitos()).contains("103034");
    }

    @Test
    @DisplayName("Nome sem números não busca por telefone; trechos muito curtos não buscam por código")
    void semCodigo() {
        assertThat(new TermoBusca("josé").digitos()).isEmpty();
        assertThat(new TermoBusca("a-1").alfanumerico()).isEmpty();
    }

    @Test
    @DisplayName("Busca em branco é ignorada")
    void branco() {
        assertThat(TermoBusca.de("   ")).isEmpty();
    }
}
