package br.com.superauto.crm.acesso.servico;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.entidade.Usuario;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;

/**
 * Regras puras sobre quem é "da equipe": quais papéis valem, qual prevalece quando há mais de um e
 * quais contas são técnicas (nunca aparecem nem são geridas pela tela de usuários).
 */
public final class RegrasEquipe {

    private static final String USERNAME_IMPORTACAO = "sistema.importacao";
    private static final String PREFIXO_CONTA_DE_SERVICO = "service-account-";

    private RegrasEquipe() {
    }

    /** Normaliza o papel informado e garante que é da equipe. Contas de cliente nascem pelo primeiro acesso. */
    public static String exigirPapelDaEquipe(String papel) {
        String normalizado = Objects.requireNonNullElse(papel, "").trim().toLowerCase(Locale.ROOT);
        if (!Papeis.EQUIPE.contains(normalizado)) {
            throw NegocioException.invalido("papel-invalido",
                    "Papel inválido. Use gerencial, vendedor ou atendente. Contas de cliente nascem pelo primeiro acesso.");
        }
        return normalizado;
    }

    /** O papel da equipe mais amplo entre os informados ({@link Papeis#EQUIPE} está do mais amplo ao mais restrito). */
    public static Optional<String> papelPrincipal(Collection<String> papeis) {
        return Papeis.EQUIPE.stream().filter(papeis::contains).findFirst();
    }

    /** É preciso mexer nos papéis quando o papel muda ou quando a conta acumula mais de um papel da equipe. */
    public static boolean precisaTrocarPapel(Collection<String> atuais, String novo) {
        return !novo.equals(papelPrincipal(atuais).orElse(null)) || quantosPapeisDaEquipe(atuais) > 1;
    }

    /** Cada pessoa da equipe tem um único papel: os demais papéis da equipe que ela tem saem. */
    public static List<String> papeisASubstituir(Collection<String> atuais, String novo) {
        return Papeis.EQUIPE.stream().filter(p -> !p.equals(novo) && atuais.contains(p)).toList();
    }

    private static long quantosPapeisDaEquipe(Collection<String> papeis) {
        return papeis.stream().filter(Papeis.EQUIPE::contains).count();
    }

    /** Usuário técnico da importação e contas de serviço dos clients do Keycloak. */
    public static boolean ehContaTecnica(Conta conta) {
        return Usuario.SISTEMA_IMPORTACAO.equals(conta.id())
                || USERNAME_IMPORTACAO.equals(conta.username())
                || (conta.username() != null && conta.username().startsWith(PREFIXO_CONTA_DE_SERVICO));
    }
}
