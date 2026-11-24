package com.onlymymoney.domain.model;

import java.math.BigDecimal;

/**
 * Domain model of a category, which belongs to an account and keeps its own available balance.
 *
 * @param id               the category identifier ({@code null} before it is persisted)
 * @param accountId        the account the category belongs to
 * @param description      the category name or description
 * @param availableBalance the balance currently available in the category
 * @param generalExpenses  {@code true} if this is the account's default "general expenses" category
 */
public record Category(Long id, Long accountId, String description, BigDecimal availableBalance,
                       boolean generalExpenses) {
}