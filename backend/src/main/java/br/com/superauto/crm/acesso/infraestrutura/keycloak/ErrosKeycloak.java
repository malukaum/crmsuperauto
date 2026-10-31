package br.com.superauto.crm.acesso.infraestrutura.keycloak;

import java.util.function.Supplier;

import br.com.superauto.crm.erro.IntegracaoIndisponivelException;
import br.com.superauto.crm.erro.NegocioException;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Parte da camada anticorrupção: traduz as respostas de erro da Admin API do Keycloak para os erros do CRM.
 * Regra de negócio vira {@link NegocioException}; falha de comunicação vira {@link IntegracaoIndisponivelException}.
 */
final class ErrosKeycloak {

    private ErrosKeycloak() {
    }

    /** @param aoRecusarCredencial chamado quando o Keycloak recusa o token de serviço (401) */
    static <T> T traduzir(Supplier<T> chamada, Runnable aoRecusarCredencial) {
        try {
            return chamada.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw NegocioException.naoEncontrado("Usuário não encontrado.");
        } catch (HttpClientErrorException.Conflict e) {
            throw NegocioException.conflito("usuario-duplicado", "Já existe um usuário com este username ou e-mail.");
        } catch (HttpClientErrorException.Unauthorized e) {
            aoRecusarCredencial.run();
            throw new IntegracaoIndisponivelException(
                    "O CRM não conseguiu se autenticar no Keycloak. Verifique a conta de serviço.", e);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && e.getResponseBodyAsString().contains("email")) {
                throw NegocioException.invalido("email-invalido", "O Keycloak recusou o e-mail informado.");
            }
            throw new IntegracaoIndisponivelException("O Keycloak recusou a operação (" + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            throw new IntegracaoIndisponivelException(
                    "Não foi possível falar com o Keycloak agora. Tente novamente em instantes.", e);
        }
    }
}
