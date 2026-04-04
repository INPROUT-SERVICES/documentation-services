package br.com.inproutservices.documentation_service.controllers;

import br.com.inproutservices.documentation_service.dtos.responses.ApiErrorResponse;
import br.com.inproutservices.documentation_service.entities.SystemErrorLog;
import br.com.inproutservices.documentation_service.repositories.SystemErrorLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SystemErrorLogRepository systemErrorLogRepository;

    public GlobalExceptionHandler(SystemErrorLogRepository systemErrorLogRepository) {
        this.systemErrorLogRepository = systemErrorLogRepository;
    }

    private void registrarErro(HttpServletRequest request, int statusCode, String erroTipo,
                                String mensagem, String detalhes) {
        try {
            SystemErrorLog log = new SystemErrorLog();
            log.setStatusCode(statusCode);
            log.setMetodoHttp(request != null ? request.getMethod() : null);
            log.setUri(request != null ? request.getRequestURI() : null);
            log.setErroTipo(erroTipo);
            log.setMensagem(mensagem != null && mensagem.length() > 2000
                    ? mensagem.substring(0, 2000) : mensagem);
            log.setDetalhes(detalhes != null && detalhes.length() > 5000
                    ? detalhes.substring(0, 5000) : detalhes);

            if (request != null) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                log.setIpAddress(xForwardedFor != null && !xForwardedFor.isBlank()
                        ? xForwardedFor.split(",")[0].trim()
                        : request.getRemoteAddr());
            }

            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof String email) {
                    log.setUsuarioEmail(email);
                }
            } catch (Exception ignored) {}

            systemErrorLogRepository.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStackTraceResumo(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();
        return full.length() > 5000 ? full.substring(0, 5000) : full;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        registrarErro(req, 403, "ACCESS_DENIED", "Acesso negado", null);
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "Você não tem permissão para executar esta ação.",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        registrarErro(req, 400, "RUNTIME", ex.getMessage(), getStackTraceResumo(ex));
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        ex.printStackTrace();
        registrarErro(req, 500, "INTERNAL_ERROR",
                ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                getStackTraceResumo(ex));
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Erro inesperado.",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
