package br.com.superauto.crm.compartilhado.entidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoTest {

    private static final String CPF_VALIDO = "52998224725";
    private static final String CNPJ_VALIDO = "11222333000181";
    /** Exemplo oficial da Receita Federal para o CNPJ alfanumérico. */
    private static final String CNPJ_ALFANUMERICO = "12ABC34501DE35";

    @Test
    @DisplayName("CPF digitado com pontuação é gravado só com dígitos e é pessoa física")
    void cpf() {
        var documento = Documento.de("529.982.247-25");

        assertThat(documento.valor()).isEqualTo(CPF_VALIDO);
        assertThat(documento.pessoaFisica()).isTrue();
        assertThat(documento.formatado()).isEqualTo("529.982.247-25");
    }

    @Test
    @DisplayName("CNPJ é pessoa jurídica e é formatado como 00.000.000/0000-00")
    void cnpj() {
        var documento = Documento.de("11.222.333/0001-81");

        assertThat(documento.valor()).isEqualTo(CNPJ_VALIDO);
        assertThat(documento.pessoaFisica()).isFalse();
        assertThat(documento.formatado()).isEqualTo("11.222.333/0001-81");
    }

    @Test
    @DisplayName("CNPJ alfanumérico é aceito, inclusive digitado em minúsculas")
    void cnpjAlfanumerico() {
        assertThat(Documento.de("12.abc.345/01de-35").valor()).isEqualTo(CNPJ_ALFANUMERICO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "11.222.333/0001-80", "111.111.111-11", "123", "12ABC34501DE36"})
    @DisplayName("Dígito verificador errado, documento repetido ou fora do formato é recusado (proposta)")
    void invalido(String digitado) {
        assertThatThrownBy(() -> Documento.de(digitado))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("documento-invalido"));
    }

    @Test
    @DisplayName("RG-04: o atendente vê o CPF mascarado como ***.***.247-**")
    void cpfMascarado() {
        assertThat(new Documento(CPF_VALIDO).mascarado()).isEqualTo("***.***.247-**");
    }

    @Test
    @DisplayName("RG-04: o CNPJ mascarado mostra só a filial")
    void cnpjMascarado() {
        assertThat(new Documento(CNPJ_VALIDO).mascarado()).isEqualTo("**.***.***/0001-**");
    }
}
