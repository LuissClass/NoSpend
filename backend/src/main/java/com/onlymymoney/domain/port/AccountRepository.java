package com.onlymymoney.domain.port;

import com.onlymymoney.domain.model.Account;

import java.util.*;

/**
 * Domain port for storing and retrieving {@link Account} objects.
 * It is implemented by the persistence adapter.
 */
public interface AccountRepository {
    /**
     * Finds an account by its identifier.
     *
     * @param id the account identifier
     * @return the account, or an empty {@link Optional} if it does not exist
     */
    Optional<Account> findById(Long id);

    /**
     * Returns all the accounts.
     *
     * @return every stored account
     */
    List<Account> findAll();

    /**
     * Inserts or updates an account.
     *
     * @param a the account to save
     * @return the saved account, including its generated id
     */
    Account save(Account a);
}