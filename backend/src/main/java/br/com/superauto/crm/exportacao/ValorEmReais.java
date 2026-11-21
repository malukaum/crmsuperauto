package br.com.superauto.crm.exportacao;

import java.math.BigDecimal;
import java.util.Objects;

/** Valor em reais numa planilha exportada: número no Excel, R$ 1.234,56 no CSV (RG-02). */
public record ValorEmReais(BigDecimal valor) {

    public ValorEmReais {
        Objects.requireNonNull(valor, "valor");
    }
}
