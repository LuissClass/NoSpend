package com.onlymymoney.domain.port;

import com.onlymymoney.domain.model.Movement;

import java.util.*;

/**
 * Domain port for storing and retrieving {@link Movement} objects.
 * It is implemented by the persistence adapter.
 */
public interface MovementRepository {
    /**
     * Checks whether an account already has a movement with the given available balance.
     * It is used to detect duplicated movements during an import.
     *
     * @param accountId the account identifier
     * @param balance   the available balance to look for
     * @return {@code true} if such a movement exists
     */
    boolean existsByAvailableBalance(Long accountId, java.math.BigDecimal balance);

    /**
     * Inserts or updates a movement.
     *
     * @param m the movement to save
     * @return the saved movement, including its generated id
     */
    Movement save(Movement m);

    /**
     * Returns the latest movements of an account, newest first.
     *
     * @param accountId  the account identifier
     * @param limit      maximum number of movements to return
     * @param categoryId optional category filter; may be {@code null} to include every category
     * @return the latest movements matching the criteria
     */
    List<Movement> findLatest(Long accountId, int limit, Long categoryId);

    /**
     * Returns every movement of an account that belongs to a category.
     *
     * @param accountId  the account identifier
     * @param categoryId the category identifier
     * @return all the matching movements
     */
    List<Movement> findAllByCategory(Long accountId, Long categoryId);

    /**
     * Finds a movement by its identifier.
     *
     * @param id the movement identifier
     * @return the movement, or an empty {@link Optional} if it does not exist
     */
    Optional<Movement> findById(Long id);

    /**
     * Counts the movements of an account.
     *
     * @param accountId the account identifier
     * @return the number of movements
     */
    long count(Long accountId);
}