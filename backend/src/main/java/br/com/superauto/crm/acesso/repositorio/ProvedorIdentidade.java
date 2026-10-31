package br.com.superauto.crm.acesso.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.compartilhado.entidade.Email;

/**
 * Onde vivem as contas de login e os papéis (hoje, o Keycloak). A gestão de usuários depende só
 * desta interface; a implementação traduz para a API do provedor e para os erros do CRM
 * ({@code NegocioException} para regra, {@code IntegracaoIndisponivelException} para falha de comunicação).
 */
public interface ProvedorIdentidade {

    /** Contas (ativas e desativadas) que têm o papel do realm informado. */
    List<Conta> contasComPapel(String papel);

    Optional<Conta> buscar(UUID id);

    /** Nomes dos papéis do realm atribuídos diretamente à conta. */
    List<String> papeis(UUID id);

    /** Cria a conta ativa, já marcada para definir a senha no primeiro acesso, e devolve o id gerado. */
    UUID criar(NovaConta nova);

    void definirAtivo(UUID id, boolean ativo);

    /** Derruba as sessões abertas: o próximo refresh de token falha e a pessoa volta ao login. */
    void encerrarSessoes(UUID id);

    void adicionarPapeis(UUID id, Collection<String> papeis);

    void removerPapeis(UUID id, Collection<String> papeis);

    /** Envia o e-mail com o link para definir a senha. */
    void enviarConviteDefinirSenha(UUID id);

    record NovaConta(String username, String nome, String sobrenome, Email email) {
    }
}
