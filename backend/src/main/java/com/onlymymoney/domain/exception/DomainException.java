package com.onlymymoney.domain.exception;

/**
 * Unchecked exception thrown when a business rule of the domain is violated
 * (for example, a missing resource or an invalid operation).
 * <p>
 * The web layer translates it into an HTTP 400 (Bad Request) response.
 */
public class DomainException extends RuntimeException {
    /**
     * Creates the exception with a descriptive message.
     *
     * @param message the reason the rule was violated, returned to the client
     */
    public DomainException(String message) {
        super(message);
    }
}