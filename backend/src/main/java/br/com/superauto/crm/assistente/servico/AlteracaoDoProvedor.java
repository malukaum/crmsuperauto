package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.AjusteDoProvedor;

import java.util.Objects;
import java.util.Optional;

/** IA-05 · O ajuste de um provedor e, se informada, uma chave nova (em claro; é cifrada antes de gravar). */
public record AlteracaoDoProvedor(AjusteDoProvedor ajuste, Optional<String> novaChave) {

    public AlteracaoDoProvedor {
        Objects.requireNonNull(ajuste, "ajuste");
        novaChave = novaChave.map(String::strip).filter(c -> !c.isEmpty());
    }

    @Override
    public String toString() {
        return "AlteracaoDoProvedor[ajuste=" + ajuste + ", novaChave=" + (novaChave.isPresent() ? "***" : "-") + "]";
    }
}
