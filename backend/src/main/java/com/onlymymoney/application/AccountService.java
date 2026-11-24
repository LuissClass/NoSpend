package com.onlymymoney.application;

import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Application service that manages accounts.
 * <p>
 * It coordinates the account and category repositories, since every new account
 * is created together with its default "general expenses" category.
 */
@Service
public class AccountService {
    private final AccountRepository accounts;
    private final CategoryRepository categories;

    /**
     * Creates the service with its collaborators (injected by Spring).
     *
     * @param a the account repository
     * @param c the category repository
     */
    public AccountService(AccountRepository a, CategoryRepository c) {
        accounts = a;
        categories = c;
    }

    /**
     * Lists all the accounts.
     *
     * @return every existing account
     */
    public List<Account> all() {
        return accounts.findAll();
    }

    /**
     * Retrieves an account by its identifier.
     *
     * @param id the account identifier
     * @return the account
     * @throws NoSuchElementException if no account exists with that identifier
     */
    public Account get(Long id) {
        return accounts.findById(id).orElseThrow();
    }

    /**
     * Creates a new account with a zero balance and the current time as its last modification.
     * <p>
     * A default category named "EXPENSES" is also created for the account and flagged as the
     * general expenses category, which is the one used when movements are imported.
     *
     * @param holder the name of the account holder
     * @return the created account, including its generated id
     */
    public Account create(String holder) {
        // Persist the account first so its generated id can be used by the default category.
        Account a = accounts.save(new Account(null, holder, Instant.now(), BigDecimal.ZERO));
        categories.save(new Category(null, a.id(), "EXPENSES", BigDecimal.ZERO, true));
        return a;
    }

    /**
     * Lists the categories of an account.
     *
     * @param id the account identifier
     * @return the categories that belong to the account
     */
    public List<Category> categories(Long id) {
        return categories.findByAccount(id);
    }
}