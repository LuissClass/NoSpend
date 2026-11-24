package com.onlymymoney.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain model of a bank account.
 *
 * @param id               the account identifier ({@code null} before it is persisted)
 * @param holder           the name of the account holder
 * @param lastModification the moment the account was last modified
 * @param totalBalance     the total balance of the account
 */
public record Account(Long id, String holder, Instant lastModification, BigDecimal totalBalance) {
}