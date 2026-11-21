package br.com.superauto.crm.relacionamento.entidade;

/**
 * ATD-05 · A próxima ação mostrada no histórico, calculada como na macro: no pós-venda, Concluído → "Retorno
 * automático"; Sem resposta e Reagendado → "Nova tentativa". Aniversário e avulsos não mexem na régua.
 */
public enum ProximaAcao {
    RETORNO_AUTOMATICO("Retorno automático"),
    NOVA_TENTATIVA("Nova tentativa"),
    SEM_ALTERACAO("Sem alteração no retorno");

    private final String descricao;

    ProximaAcao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public static ProximaAcao de(TipoDeRelacionamento tipo, ResultadoContato resultado) {
        if (!tipo.moveRegua()) {
            return SEM_ALTERACAO;
        }
        return switch (resultado) {
            case CONCLUIDO -> RETORNO_AUTOMATICO;
            case SEM_RESPOSTA, REAGENDADO -> NOVA_TENTATIVA;
            case PENDENTE -> SEM_ALTERACAO;
        };
    }
}
