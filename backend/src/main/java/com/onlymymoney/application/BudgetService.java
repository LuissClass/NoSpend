package com.onlymymoney.application;

import com.onlymymoney.domain.exception.DomainException;
import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Application service that manages budgets.
 * <p>
 * A budget assigns an amount to a category for a given period of time.
 */
@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;

    /**
     * Creates the service with its collaborators (injected by Spring).
     *
     * @param b the budget repository
     * @param c the category repository
     */
    public BudgetService(BudgetRepository b, CategoryRepository c) {
        budgets = b;
        categories = c;
    }

    /**
     * Creates a budget for a category.
     * <p>
     * The budget is rejected if the category does not belong to the given account,
     * if the end date is before the start date, or if the amount is negative.
     *
     * @param accountId  the account the category must belong to
     * @param categoryId the category the budget applies to
     * @param start      the first day of the budget period
     * @param end        the last day of the budget period
     * @param amount     the budgeted amount (zero or greater)
     * @return the created budget, including its generated id
     * @throws DomainException if the category does not exist or the budget is invalid
     */
    public Budget create(Long accountId, Long categoryId, LocalDate start, LocalDate end, BigDecimal amount) {
        Category c = categories.findById(categoryId).orElseThrow(() -> new DomainException("Category not found"));
        // Validate ownership, date range and non-negative amount in a single check.
        if (!c.accountId().equals(accountId) || end.isBefore(start) || amount.signum() < 0)
            throw new DomainException("Invalid budget");
        return budgets.save(new Budget(null, categoryId, start, end, amount));
    }

    /**
     * Lists the budgets of a category.
     *
     * @param categoryId the category identifier
     * @return the budgets defined for the category
     */
    public List<Budget> list(Long categoryId) {
        return budgets.findByCategory(categoryId);
    }
}