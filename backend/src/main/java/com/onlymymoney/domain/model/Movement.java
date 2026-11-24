package com.onlymymoney.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model of a bank movement.
 *
 * @param id               the movement identifier ({@code null} before it is persisted)
 * @param accountId        the account the movement belongs to
 * @param concept          the concept or description of the movement
 * @param date             the date on which the movement took place
 * @param amount           the amount; negative values are expenses and positive values are income
 * @param availableBalance the available balance reported after the movement
 * @param status           the import status of the movement
 * @param categoryId       the category assigned to the movement, or {@code null} if none
 */
public record Movement(Long id, Long accountId, String concept, LocalDate date, BigDecimal amount,
                       BigDecimal availableBalance, ImportStatus status, Long categoryId) {
    /**
     * Tells whether the movement is an expense, i.e. its amount is negative.
     *
     * @return {@code true} if the amount is negative
     * @throws NullPointerException if the amount is {@code null}
     */
    public boolean isExpense() {
        return amount.signum() < 0;
    }
}