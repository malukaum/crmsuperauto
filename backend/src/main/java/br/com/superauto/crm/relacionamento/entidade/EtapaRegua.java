package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.erro.NegocioException;

import java.util.List;

/**
 * Uma etapa da régua de pós-venda como o Relacionamento a usa (RG-13).
 *
 * @param diasAposAnterior prazo contado do contato anterior concluído, ou da adesão na etapa inicial
 * @param proximaEtapaId etapa que fica pendente quando esta é concluída (o Trimestral aponta para si mesmo)
 */
public record EtapaRegua(Short id, String codigo, String nome, int diasAposAnterior, Short proximaEtapaId,
                         boolean inicial) {

    /** A etapa com o id dado; recusa uma etapa que não existe na régua. */
    public static EtapaRegua daRegua(List<EtapaRegua> regua, Short id) {
        return regua.stream()
                .filter(etapa -> etapa.id().equals(id))
                .findFirst()
                .orElseThrow(() -> NegocioException.invalido("etapa-inexistente", "Etapa não encontrada na régua."));
    }
}
