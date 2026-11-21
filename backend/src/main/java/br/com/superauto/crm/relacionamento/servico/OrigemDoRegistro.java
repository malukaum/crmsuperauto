package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;

/**
 * ATD-01 · De onde vem o registro de pós-venda. Pela fila, o contrato precisa continuar nela (A conferir ou Ativo);
 * pela ficha, até o suspenso pode ser registrado, como na macro, e só o cancelado é recusado.
 */
public enum OrigemDoRegistro {
    FILA {
        @Override
        void exigirDisponivel(ContratoNaFila contrato) {
            contrato.exigirPosVendaDisponivel();
        }
    },
    FICHA {
        @Override
        void exigirDisponivel(ContratoNaFila contrato) {
            contrato.exigirPosVendaPelaFicha();
        }
    };

    abstract void exigirDisponivel(ContratoNaFila contrato);
}
