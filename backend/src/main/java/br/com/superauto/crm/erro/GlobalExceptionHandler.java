package br.com.superauto.crm.erro;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Todas as respostas de erro da API em ProblemDetail (RFC 9457, sucessora da RFC 7807), em português.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<Integer, String> TITULOS = Map.ofEntries(
            Map.entry(400, "Requisição inválida"),
            Map.entry(401, "Não autenticado"),
            Map.entry(403, "Acesso negado"),
            Map.entry(404, "Recurso não encontrado"),
            Map.entry(405, "Método não permitido"),
            Map.entry(406, "Formato de resposta não suportado"),
            Map.entry(409, "Conflito"),
            Map.entry(415, "Tipo de conteúdo não suportado"),
            Map.entry(422, "Dados inválidos"),
            Map.entry(500, "Erro interno"),
            Map.entry(503, "Serviço indisponível"));

    private static final Map<Integer, String> DETALHES = Map.of(
            400, "A requisição não pôde ser processada. Verifique os dados enviados.",
            404, "O endereço solicitado não existe.",
            405, "Este endereço não aceita o método HTTP utilizado.",
            406, "Não é possível responder no formato solicitado.",
            415, "O tipo de conteúdo enviado não é aceito por este endereço.");

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail naoAutenticado(AuthenticationException ex, HttpServletRequest request) {
        String detalhe = ex instanceof InvalidBearerTokenException
                ? "Token de acesso inválido ou expirado. Faça login novamente."
                : "É necessário estar autenticado para acessar este recurso.";
        return problema(HttpStatus.UNAUTHORIZED, detalhe, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return problema(HttpStatus.FORBIDDEN,
                "Seu perfil não tem permissão para acessar este recurso.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail erroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.", request.getRequestURI());
    }

    /** Traduz os erros padrão do Spring MVC (404, 405, 400...) mantendo o formato ProblemDetail. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail pd) {
            traduzir(pd, statusCode.value());
            completar(pd, request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ProblemDetail problema(HttpStatus status, String detalhe, String rota) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setTitle(TITULOS.getOrDefault(status.value(), status.getReasonPhrase()));
        completar(pd, rota);
        return pd;
    }

    private void traduzir(ProblemDetail pd, int status) {
        pd.setTitle(TITULOS.getOrDefault(status, pd.getTitle()));
        if (DETALHES.containsKey(status)) {
            pd.setDetail(DETALHES.get(status));
        }
    }

    private void completar(ProblemDetail pd, String rota) {
        if (rota != null) {
            pd.setInstance(URI.create(rota));
        }
        pd.setProperty("dataHora", OffsetDateTime.now(clock).toString());
    }
}
