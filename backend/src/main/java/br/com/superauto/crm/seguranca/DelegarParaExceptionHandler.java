package br.com.superauto.crm.seguranca;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Erros de autenticação (401) e autorização (403) que acontecem na cadeia de filtros do Spring Security
 * não passam pelo {@code @RestControllerAdvice}. Esta classe repassa a exceção para o resolver do MVC,
 * de forma que todas as respostas de erro saem no mesmo formato ProblemDetail, em português.
 */
public class DelegarParaExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public DelegarParaExceptionHandler(HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        resolver.resolveException(request, response, null, ex);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        resolver.resolveException(request, response, null, ex);
    }
}
