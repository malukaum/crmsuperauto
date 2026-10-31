package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** FIL-06: a configuração muda os prazos de todas as etapas, preservando a sequência. */
public record PrazosDaRegua(Map<Short, Integer> dias) {
    public PrazosDaRegua {
        if (dias == null
                || dias.isEmpty()
                || dias.values().stream().anyMatch(d -> d == null || d < 1 || d > 365)) {
            throw NegocioException.invalido(
                    "prazos-invalidos", "Informe todas as etapas, com prazos entre 1 e 365 dias.");
        }
        dias = Map.copyOf(dias);
    }

    public List<EtapaRegua> aplicar(List<EtapaRegua> atuais) {
        if (!dias.keySet().equals(new HashSet<>(atuais.stream().map(EtapaRegua::id).toList()))) {
            throw NegocioException.invalido(
                    "etapas-invalidas",
                    "A sequência das etapas é fixa. Informe os prazos de todas as etapas.");
        }
        return atuais.stream().map(this::comNovoPrazo).toList();
    }

    private EtapaRegua comNovoPrazo(EtapaRegua etapa) {
        return new EtapaRegua(
                etapa.id(),
                etapa.codigo(),
                etapa.nome(),
                dias.get(etapa.id()),
                etapa.proximaEtapaId(),
                etapa.inicial());
    }
}
