package com.onlymymoney.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model of a budget: an amount assigned to a category for a given period.
 *
 * @param id          the budget identifier ({@code null} before it is persisted)
 * @param categoryId  the category the budget applies to
 * @param periodStart the first day of the budget period
 * @param periodEnd   the last day of the budget period
 * @param amount      the budgeted amount
 */
public record Budget(Long id, Long categoryId, LocalDate periodStart, LocalDate periodEnd, BigDecimal amount) {
}