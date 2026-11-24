package com.onlymymoney.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

/**
 * Spring Data repository for {@link CategoryJpa} entities, keyed by {@link Long}.
 */
public interface CategoryJpaRepository extends JpaRepository<CategoryJpa, Long> {
    /**
     * Finds all the categories that belong to an account (derived query).
     *
     * @param accountId the account identifier
     * @return the categories of the account
     */
    List<CategoryJpa> findByAccountId(Long accountId);

    /**
     * Finds the category of an account that is flagged as "general expenses" (derived query).
     *
     * @param accountId the account identifier
     * @return the general expenses category, or an empty {@link Optional} if there is none
     */
    Optional<CategoryJpa> findByAccountIdAndGeneralExpensesTrue(Long accountId);
}