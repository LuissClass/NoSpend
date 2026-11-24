package com.onlymymoney.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

/**
 * Spring Data repository for {@link BudgetJpa} entities, keyed by {@link Long}.
 */
public interface BudgetJpaRepository extends JpaRepository<BudgetJpa, Long> {
    /**
     * Finds all the budgets that belong to a category (derived query).
     *
     * @param categoryId the category identifier
     * @return the budgets of the category
     */
    List<BudgetJpa> findByCategoryId(Long categoryId);
}