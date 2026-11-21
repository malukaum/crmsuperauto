package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.ConfiguracaoDoProvedor;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;

import java.math.BigDecimal;

/** IA-05 · Um provedor como a tela de configuração mostra. A chave nunca volta: só os últimos caracteres. */
public record ProvedorConfigurado(ProvedorDeIa provedor, String nome, String modelo, boolean ativo, boolean comChave,
                                  String chaveFinal, BigDecimal limiteMensal, BigDecimal precoEntrada,
                                  BigDecimal precoSaida, BigDecimal gastoDoMes, int versao) {

    static ProvedorConfigurado de(ConfiguracaoDoProvedor c, BigDecimal gastoDoMes) {
        return new ProvedorConfigurado(c.getProvedor(), c.getProvedor().nome(), c.getModelo().orElse(null), c.isAtivo(),
                c.getChaveCifrada().isPresent(), c.getChaveFinal().orElse(null), c.getLimiteMensal(),
                c.getPrecoEntrada().orElse(null), c.getPrecoSaida().orElse(null), gastoDoMes, c.getVersao());
    }
}
