package br.com.superauto.crm.relacionamento.entidade;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;

/** Só para testes: monta a linha da visão de trabalho como o banco a devolveria. */
public final class ContratoNaFilaDeTeste {

    private ContratoNaFilaDeTeste() {
    }

    public static ContratoNaFila contrato(Long contratoId, Long associadoId, SituacaoNaCarteira situacao, UUID vendedor) {
        try {
            var construtor = ContratoNaFila.class.getDeclaredConstructor();
            construtor.setAccessible(true);
            var contrato = construtor.newInstance();
            definir(contrato, "contratoId", contratoId);
            definir(contrato, "associadoId", associadoId);
            definir(contrato, "situacao", situacao);
            definir(contrato, "vendedorId", vendedor);
            definir(contrato, "etapa", "D+7");
            definir(contrato, "baseRegua", LocalDate.of(2026, 3, 13));
            return contrato;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void definir(ContratoNaFila contrato, String nome, Object valor) throws ReflectiveOperationException {
        Field campo = ContratoNaFila.class.getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(contrato, valor);
    }
}
