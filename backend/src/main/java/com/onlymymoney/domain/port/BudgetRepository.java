package com.onlymymoney.domain.port;

import com.onlymymoney.domain.model.Budget;

import java.util.*;

/**
 * Domain port for storing and retrieving {@link Budget} objects.
 * It is implemented by the persistence adapter.
 */
public interface BudgetRepository {
    /**
     * Inserts or updates a budget.
     *
     * @param b the budget to save
     * @return the saved budget, including its generated id
     */
    Budget save(Budget b);

    /**
     * Lists the budgets defined for a category.
     *
     * @param categoryId the category identifier
     * @return the budgets of the category
     */
    List<Budget> findByCategory(Long categoryId);
}