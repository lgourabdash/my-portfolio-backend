package com.lgourabdash.portfolio;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Records unhandled API exceptions into {@link AdminErrorLogService} and
 * returns JSON errors for
 * /api/** failures.
 */
@RestControllerAdvice
public class PortfolioApiExceptionHandler {

    private final AdminErrorLogService errorLogService;

    public PortfolioApiExceptionHandler(AdminErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(
            ResponseStatusException ex, HttpServletRequest req) {
        int code = ex.getStatusCode().value();
        if (req.getRequestURI() != null && req.getRequestURI().startsWith("/api/")) {
            errorLogService.logBackendException(ex, req.getRequestURI(), code);
        }
        String msg = ex.getReason() != null
                ? ex.getReason()
                : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            errorLogService.logBackendException(ex, uri, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", msg));
    }
}
