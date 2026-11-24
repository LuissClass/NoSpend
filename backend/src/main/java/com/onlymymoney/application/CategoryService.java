package com.onlymymoney.application;

import com.onlymymoney.domain.exception.DomainException;
import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Application service that manages the categories of an account.
 * <p>
 * Each category keeps its own available balance, and money can be moved between categories.
 */
@Service
public class CategoryService {
    private final CategoryRepository cats;

    /**
     * Creates the service with its collaborator (injected by Spring).
     *
     * @param cats the category repository
     */
    public CategoryService(CategoryRepository cats) {
        this.cats = cats;
    }

    /**
     * Lists the categories of an account.
     *
     * @param accountId the account identifier
     * @return the categories that belong to the account
     */
    public List<Category> list(Long accountId) {
        return cats.findByAccount(accountId);
    }

    /**
     * Creates a regular category (not the general expenses one) with a zero balance.
     * The name is trimmed before being stored.
     *
     * @param accountId the account the category belongs to
     * @param name      the category name (required, must not be blank)
     * @return the created category, including its generated id
     * @throws DomainException if the name is {@code null} or blank
     */
    public Category create(Long accountId, String name) {
        if (name == null || name.isBlank()) throw new DomainException("Category name is required");
        return cats.save(new Category(null, accountId, name.trim(), BigDecimal.ZERO, false));
    }

    /**
     * Moves an amount from one category to another within the same account.
     * <p>
     * It runs in a single transaction, so either both balances are updated or none is.
     * The transfer is rejected if the amount is not positive, if either category does not exist
     * or does not belong to the account, or if the source category does not have enough balance.
     *
     * @param accountId the account both categories must belong to
     * @param fromId    the category the amount is taken from
     * @param toId      the category the amount is moved to
     * @param amount    the amount to transfer (must be greater than zero)
     * @return the updated list of categories of the account
     * @throws DomainException if any of the validations fails
     */
    @Transactional
    public List<Category> transfer(Long accountId, Long fromId, Long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new DomainException("Transfer amount must be positive");
        Category from = cats.findById(fromId).orElseThrow(() -> new DomainException("Source category not found"));
        Category to = cats.findById(toId).orElseThrow(() -> new DomainException("Target category not found"));
        // Both categories must belong to the account indicated in the request.
        if (!from.accountId().equals(accountId) || !to.accountId().equals(accountId))
            throw new DomainException("Categories do not belong to account");
        if (from.availableBalance().compareTo(amount) < 0) throw new DomainException("Insufficient available balance");
        // Subtract from the source and add to the target.
        cats.save(new Category(from.id(), from.accountId(), from.description(), from.availableBalance().subtract(amount), from.generalExpenses()));
        cats.save(new Category(to.id(), to.accountId(), to.description(), to.availableBalance().add(amount), to.generalExpenses()));
        return cats.findByAccount(accountId);
    }
}