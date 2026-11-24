package com.onlymymoney.application;

import com.onlymymoney.domain.exception.DomainException;
import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Application service that handles the import of movements from a CSV file.
 * <p>
 * The import is a two-step flow: {@link #examine(InputStream, Long)} reads and validates the file
 * without saving anything, and {@link #confirm(ImportResult, Long)} persists the accepted movements
 * and updates the account and the general expenses category.
 */
@Service
public class ImportService {
    private final CsvReader csv;
    private final MovementRepository movements;
    private final AccountRepository accounts;
    private final CategoryRepository categories;

    /**
     * Creates the service with its collaborators (injected by Spring).
     *
     * @param csv        the reader used to parse the CSV file
     * @param movements  the movement repository
     * @param accounts   the account repository
     * @param categories the category repository
     */
    public ImportService(CsvReader csv, MovementRepository movements, AccountRepository accounts, CategoryRepository categories) {
        this.csv = csv;
        this.movements = movements;
        this.accounts = accounts;
        this.categories = categories;
    }

    /**
     * Examines a CSV file and classifies its rows, without persisting anything.
     * <p>
     * A row is ignored if it has missing data (amount, available balance, date or concept), or if its
     * available balance is a duplicate, meaning it already exists in the account or appears earlier in the same file.
     * Valid rows are returned as accepted with status {@code ImportStatus.EXAMINING}.
     * <p>
     * The resulting total balance is the available balance of the last row in the file, or the current
     * account balance if the file produced no rows.
     *
     * @param in        the CSV content
     * @param accountId the account the movements will belong to
     * @return the accepted movements, the ignored ones, the error messages and the resulting total balance
     * @throws DomainException if the account does not exist
     */
    public ImportResult examine(InputStream in, Long accountId) {
        accounts.findById(accountId).orElseThrow(() -> new DomainException("Account not found"));
        List<Movement> rows = csv.read(in, accountId);
        List<Movement> accepted = new ArrayList<>(), ignored = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        // Available balances already seen in this file, used to detect duplicates inside the same batch.
        Set<BigDecimal> batchBalances = new HashSet<>();
        for (Movement r : rows) {
            // Rows with missing mandatory data are ignored.
            if (r.amount() == null || r.availableBalance() == null || r.date() == null || r.concept() == null || r.concept().isBlank()) {
                ignored.add(new Movement(r.id(), r.accountId(), r.concept(), r.date(), r.amount(), r.availableBalance(), ImportStatus.IGNORED, null));
                errors.add("Invalid movement: " + r.concept());
                continue;
            }
            // Duplicates are detected by available balance: already stored in the account, or repeated within this file.
            if (movements.existsByAvailableBalance(accountId, r.availableBalance()) || !batchBalances.add(r.availableBalance())) {
                ignored.add(new Movement(r.id(), r.accountId(), r.concept(), r.date(), r.amount(), r.availableBalance(), ImportStatus.IGNORED, null));
                errors.add("Duplicate by available balance: " + r.availableBalance());
                continue;
            }
            accepted.add(new Movement(null, accountId, r.concept(), r.date(), r.amount(), r.availableBalance(), ImportStatus.EXAMINING, null));
        }
        // The new total balance comes from the last row of the file (or the current balance if there are no rows).
        BigDecimal last = rows.isEmpty() ? accounts.findById(accountId).orElseThrow().totalBalance() : rows.get(rows.size() - 1).availableBalance();
        return new ImportResult(accepted, ignored, errors, last);
    }

    /**
     * Confirms an import previously examined, persisting its effects in a single transaction.
     * <p>
     * It saves every accepted movement with status {@code ImportStatus.ADDED_TO_MAIN_CSV} and no category,
     * updates the account balance and last modification time, and adds the sum of the accepted amounts
     * to the balance of the account's general expenses category.
     * <p>
     * The movements and the total balance are taken from {@code result} as received; they are not validated again.
     *
     * @param result    the import result to confirm
     * @param accountId the account the movements belong to
     * @return the same result that was received
     * @throws DomainException if the account or its general expenses category does not exist
     */
    @Transactional
    public ImportResult confirm(ImportResult result, Long accountId) {
        Account a = accounts.findById(accountId).orElseThrow(() -> new DomainException("Account not found"));
        // Persist each accepted movement as a definitive one.
        for (Movement m : result.accepted())
            movements.save(new Movement(null, accountId, m.concept(), m.date(), m.amount(), m.availableBalance(), ImportStatus.ADDED_TO_MAIN_CSV, null));
        // Update the account with the new total balance and the current time.
        accounts.save(new Account(a.id(), a.holder(), java.time.Instant.now(), result.totalBalance()));
        Category expenses = categories.findGeneral(accountId).orElseThrow(() -> new DomainException("EXPENSES category missing"));
        // Add the amounts of the accepted movements to the general expenses category balance.
        BigDecimal newExpenses = expenses.availableBalance();
        for (Movement m : result.accepted()) newExpenses = newExpenses.add(m.amount());
        categories.save(new Category(expenses.id(), expenses.accountId(), expenses.description(), newExpenses, true));
        return result;
    }
}