package br.com.superauto.crm.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Apresenta ao Hibernate a função {@code texto_busca(text)} do banco (sem acento e em minúsculas, RG-05), criada pelo
 * Liquibase, com retorno texto. Assim ela pode ser usada em JPQL e na Criteria API, inclusive dentro de {@code like}.
 * Registrada em {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 */
public class FuncoesDeBusca implements FunctionContributor {

    public static final String TEXTO_BUSCA = "texto_busca";

    @Override
    public void contributeFunctions(FunctionContributions funcoes) {
        var texto = funcoes.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.STRING);
        funcoes.getFunctionRegistry().registerPattern(TEXTO_BUSCA, TEXTO_BUSCA + "(?1)", texto);
    }
}
