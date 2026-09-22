package com.onlymymoney.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ExceptionHandler {

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
