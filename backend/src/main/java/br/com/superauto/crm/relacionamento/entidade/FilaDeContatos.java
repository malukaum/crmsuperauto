package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.util.Arrays;

/** Identifica as duas filas de contatos disponíveis para a equipe (FIL-02 e FIL-03). */
public enum FilaDeContatos {
    HOJE("hoje"),
    ATRASADOS("atrasados");
    private final String rota;

    FilaDeContatos(String rota) {
        this.rota = rota;
    }

    public boolean atrasados() {
        return this == ATRASADOS;
    }

    public static FilaDeContatos pelaRota(String rota) {
        return Arrays.stream(values())
                .filter(fila -> fila.rota.equals(rota))
                .findFirst()
                .orElseThrow(() -> NegocioException.invalido("fila-invalida", "Fila inválida."));
    }
}
