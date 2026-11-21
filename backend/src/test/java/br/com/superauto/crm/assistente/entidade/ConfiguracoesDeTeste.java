package br.com.superauto.crm.assistente.entidade;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/** Só para testes: monta configurações de provedor e rotas como o banco devolveria. */
public final class ConfiguracoesDeTeste {

    private ConfiguracoesDeTeste() {
    }

    public static ConfiguracaoDoProvedor provedor(ProvedorDeIa provedor, boolean ativo, String modelo, String chaveCifrada,
                                                  BigDecimal limite) {
        var c = nova(ConfiguracaoDoProvedor.class);
        definir(c, "provedor", provedor);
        definir(c, "ativo", ativo);
        definir(c, "modelo", modelo);
        definir(c, "chaveCifrada", chaveCifrada);
        definir(c, "limiteMensal", limite);
        definir(c, "precoEntrada", new BigDecimal("4"));
        definir(c, "precoSaida", new BigDecimal("20"));
        return c;
    }

    public static RotaDaTarefa rota(TarefaDeIa tarefa, ProvedorDeIa principal, ProvedorDeIa reserva) {
        var r = nova(RotaDaTarefa.class);
        definir(r, "tarefa", tarefa);
        definir(r, "principal", principal);
        definir(r, "reserva", reserva);
        return r;
    }

    private static <T> T nova(Class<T> tipo) {
        try {
            var construtor = tipo.getDeclaredConstructor();
            construtor.setAccessible(true);
            return construtor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void definir(Object alvo, String campo, Object valor) {
        try {
            Field f = alvo.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(alvo, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
