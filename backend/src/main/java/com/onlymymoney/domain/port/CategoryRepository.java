package com.onlymymoney.domain.port;

import com.onlymymoney.domain.model.Category;

import java.util.*;

/**
 * Domain port for storing and retrieving {@link Category} objects.
 * It is implemented by the persistence adapter.
 */
public interface CategoryRepository {
    /**
     * Lists the categories of an account.
     *
     * @param accountId the account identifier
     * @return the categories that belong to the account
     */
    List<Category> findByAccount(Long accountId);

    /**
     * Finds a category by its identifier.
     *
     * @param id the category identifier
     * @return the category, or an empty {@link Optional} if it does not exist
     */
    Optional<Category> findById(Long id);

    /**
     * Finds the "general expenses" category of an account.
     *
     * @param accountId the account identifier
     * @return the general expenses category, or an empty {@link Optional} if the account has none
     */
    Optional<Category> findGeneral(Long accountId);

    /**
     * Inserts or updates a category.
     *
     * @param c the category to save
     * @return the saved category, including its generated id
     */
    Category save(Category c);
}