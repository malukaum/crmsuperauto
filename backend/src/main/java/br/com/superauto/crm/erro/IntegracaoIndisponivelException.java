package br.com.superauto.crm.erro;

/** Um serviço externo (ex.: Keycloak) não respondeu como esperado. Sai como 503. */
public class IntegracaoIndisponivelException extends RuntimeException {

    public IntegracaoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
