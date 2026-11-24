package com.onlymymoney.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for the REST API.
 * <p>
 * It intercepts exceptions thrown by any controller and converts them into consistent HTTP responses.
 */
@RestControllerAdvice
public class ExceptionHandler {

    /**
     * Handles invalid-request errors and domain rule violations.
     * <p>
     * Both {@link IllegalArgumentException} and {@code DomainException} are translated into an
     * HTTP 400 (Bad Request) response with a JSON body of the form {@code {"error": "<message>"}}.
     * If the exception has no message, the generic text "Invalid request" is used instead.
     *
     * @param e the exception that was thrown
     * @return a 400 response containing the error message
     */
    @org.springframework.web.bind.annotation.ExceptionHandler({
            IllegalArgumentException.class,
            com.onlymymoney.domain.exception.DomainException.class
    })
    ResponseEntity<Map<String, String>> bad(RuntimeException e) {
        return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage() == null ? "Invalid request" : e.getMessage())
        );
    }
}