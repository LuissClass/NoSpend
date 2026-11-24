package com.onlymymoney.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.*;

/**
 * Spring Data repository for {@link MovementJpa} entities, keyed by {@link Long}.
 * <p>
 * All the queries are derived from the method names.
 */
public interface MovementJpaRepository extends JpaRepository<MovementJpa, Long> {
    /**
     * Checks whether an account has a movement with the given available balance.
     *
     * @param accountId the account identifier
     * @param balance   the available balance to look for
     * @return {@code true} if such a movement exists
     */
    boolean existsByAccountIdAndAvailableBalance(Long accountId, BigDecimal balance);

    /**
     * Returns the 50 latest movements of an account within a category,
     * ordered by movement date and then by id, both descending.
     *
     * @param accountId  the account identifier
     * @param categoryId the category identifier
     * @return up to 50 movements, newest first
     */
    List<MovementJpa> findTop50ByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(Long accountId, Long categoryId);

    /**
     * Returns the 50 latest movements of an account (any category),
     * ordered by movement date and then by id, both descending.
     *
     * @param accountId the account identifier
     * @return up to 50 movements, newest first
     */
    List<MovementJpa> findTop50ByAccountIdOrderByMovementDateDescIdDesc(Long accountId);

    /**
     * Returns every movement of an account within a category,
     * ordered by movement date and then by id, both descending.
     *
     * @param accountId  the account identifier
     * @param categoryId the category identifier
     * @return all the matching movements, newest first
     */
    List<MovementJpa> findByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(Long accountId, Long categoryId);

    /**
     * Counts the movements of an account.
     *
     * @param accountId the account identifier
     * @return the number of movements
     */
    long countByAccountId(Long accountId);
}