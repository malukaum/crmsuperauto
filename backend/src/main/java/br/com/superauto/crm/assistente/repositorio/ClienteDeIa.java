package br.com.superauto.crm.assistente.repositorio;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;

import java.util.Objects;

/**
 * IA-05 · Porta para um provedor de IA contratado. Cada provedor (Claude, Gemini, ChatGPT) tem um adaptador na
 * infraestrutura, com o SDK oficial dele; o resto do sistema só conhece esta interface.
 *
 * <p>Contrato: devolve o texto da resposta e os tokens usados. Qualquer problema (fora do ar, chave inválida,
 * recusa, resposta vazia ou cortada) vira {@link FalhaNoProvedor}, para o assistente tentar o reserva.
 */
public interface ClienteDeIa {

    ProvedorDeIa provedor();

    RespostaDaIa gerar(PedidoDaIa pedido, AcessoAoProvedor acesso);

    /** @param instrucoes o papel e as regras (system); @param conteudo o material já pseudonimizado */
    record PedidoDaIa(String instrucoes, String conteudo, long limiteDeTokensNaResposta) {
        public PedidoDaIa {
            Objects.requireNonNull(instrucoes, "instrucoes");
            Objects.requireNonNull(conteudo, "conteudo");
        }
    }

    /** Modelo e chave em claro, montados só no servidor, só para a chamada. */
    record AcessoAoProvedor(String modelo, String chave) {
        @Override
        public String toString() {
            return "AcessoAoProvedor[modelo=" + modelo + ", chave=***]";
        }
    }

    record RespostaDaIa(String texto, long tokensEntrada, long tokensSaida) {}

    /** O provedor não entregou uma resposta utilizável. */
    class FalhaNoProvedor extends RuntimeException {
        public FalhaNoProvedor(String mensagem) {
            super(mensagem);
        }

        public FalhaNoProvedor(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }
}
