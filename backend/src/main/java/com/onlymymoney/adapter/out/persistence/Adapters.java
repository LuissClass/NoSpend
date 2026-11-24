package com.onlymymoney.adapter.out.persistence;

import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Outbound adapter that implements the {@link AccountRepository} domain port using Spring Data JPA.
 * <p>
 * It converts between the domain {@link Account} and the {@link AccountJpa} entity.
 */
@Component
class AccountPersistenceAdapter implements AccountRepository {
    private final AccountJpaRepository accountJpaRepository;

    /**
     * Creates the adapter with the JPA repository it delegates to (injected by Spring).
     *
     * @param accountJpaRepository the Spring Data repository for accounts
     */
    AccountPersistenceAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    /**
     * Finds an account by its identifier.
     *
     * @param accountId the account identifier
     * @return the account, or an empty {@link Optional} if it does not exist
     */
    public Optional<Account> findById(Long accountId) {
        return accountJpaRepository.findById(accountId).map(this::toDomain);
    }

    /**
     * Returns all the accounts.
     *
     * @return the list of every stored account
     */
    public List<Account> findAll() {
        return accountJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    /**
     * Inserts or updates an account (an update happens when the account already has an id).
     *
     * @param account the account to save
     * @return the saved account, including its generated id
     */
    public Account save(Account account) {
        return toDomain(accountJpaRepository.save(new AccountJpa(account.id(), account.holder(), account.totalBalance(), account.lastModification())));
    }

    /**
     * Maps a JPA entity to the domain model.
     * Note that the {@link Account} constructor expects the last modification before the total balance.
     *
     * @param accountJpa the JPA entity
     * @return the equivalent domain object
     */
    private Account toDomain(AccountJpa accountJpa) {
        return new Account(accountJpa.getId(), accountJpa.getHolder(), accountJpa.getLastModification(), accountJpa.getTotalBalance());
    }
}

/**
 * Outbound adapter that implements the {@link CategoryRepository} domain port using Spring Data JPA.
 * <p>
 * It converts between the domain {@link Category} and the {@link CategoryJpa} entity.
 */
@Component
class CategoryPersistenceAdapter implements CategoryRepository {
    private final CategoryJpaRepository categoryJpaRepository;

    /**
     * Creates the adapter with the JPA repository it delegates to (injected by Spring).
     *
     * @param categoryJpaRepository the Spring Data repository for categories
     */
    CategoryPersistenceAdapter(CategoryJpaRepository categoryJpaRepository) {
        this.categoryJpaRepository = categoryJpaRepository;
    }

    /**
     * Lists the categories that belong to an account.
     *
     * @param accountId the account identifier
     * @return the categories of the account
     */
    public List<Category> findByAccount(Long accountId) {
        return categoryJpaRepository.findByAccountId(accountId).stream().map(this::toDomain).toList();
    }

    /**
     * Finds a category by its identifier.
     *
     * @param categoryId the category identifier
     * @return the category, or an empty {@link Optional} if it does not exist
     */
    public Optional<Category> findById(Long categoryId) {
        return categoryJpaRepository.findById(categoryId).map(this::toDomain);
    }

    /**
     * Finds the "general expenses" category of an account, i.e. the one flagged as such.
     *
     * @param accountId the account identifier
     * @return the general expenses category, or an empty {@link Optional} if the account has none
     */
    public Optional<Category> findGeneral(Long accountId) {
        return categoryJpaRepository.findByAccountIdAndGeneralExpensesTrue(accountId).map(this::toDomain);
    }

    /**
     * Inserts or updates a category (an update happens when the category already has an id).
     *
     * @param category the category to save
     * @return the saved category, including its generated id
     */
    public Category save(Category category) {
        return toDomain(categoryJpaRepository.save(new CategoryJpa(category.id(), category.accountId(), category.description(), category.availableBalance(), category.generalExpenses())));
    }

    /**
     * Maps a JPA entity to the domain model.
     *
     * @param categoryJpa the JPA entity
     * @return the equivalent domain object
     */
    private Category toDomain(CategoryJpa categoryJpa) {
        return new Category(categoryJpa.getId(), categoryJpa.getAccountId(), categoryJpa.getDescription(), categoryJpa.getAvailableBalance(), categoryJpa.isGeneralExpenses());
    }
}

/**
 * Outbound adapter that implements the {@link MovementRepository} domain port using Spring Data JPA.
 * <p>
 * It converts between the domain {@link Movement} and the {@link MovementJpa} entity.
 */
@Component
class MovementPersistenceAdapter implements MovementRepository {
    private final MovementJpaRepository movementJpaRepository;

    /**
     * Creates the adapter with the JPA repository it delegates to (injected by Spring).
     *
     * @param movementJpaRepository the Spring Data repository for movements
     */
    MovementPersistenceAdapter(MovementJpaRepository movementJpaRepository) {
        this.movementJpaRepository = movementJpaRepository;
    }

    /**
     * Checks whether the account already has a movement with the given available balance.
     *
     * @param accountId        the account identifier
     * @param availableBalance the available balance to look for
     * @return {@code true} if such a movement exists
     */
    public boolean existsByAvailableBalance(Long accountId, java.math.BigDecimal availableBalance) {
        return movementJpaRepository.existsByAccountIdAndAvailableBalance(accountId, availableBalance);
    }

    /**
     * Inserts or updates a movement (an update happens when the movement already has an id).
     * <p>
     * The creation timestamp is generated with {@code Instant.now()} on every save.
     *
     * @param movement the movement to save
     * @return the saved movement, including its generated id
     */
    public Movement save(Movement movement) {
        return toDomain(movementJpaRepository.save(new MovementJpa(movement.id(), movement.accountId(), movement.concept(), movement.date(), movement.amount(), movement.availableBalance(), movement.status(), movement.categoryId(), java.time.Instant.now())));
    }

    /**
     * Returns the latest movements of an account, optionally filtered by category.
     * <p>
     * Results are ordered by movement date and then by id, both descending. The query itself
     * returns at most 50 rows; {@code limit} is then applied in memory on top of that.
     *
     * @param accountId  the account identifier
     * @param limit      maximum number of movements to return
     * @param categoryId the category identifier, or {@code null} to include every category
     * @return the latest movements matching the criteria
     */
    public List<Movement> findLatest(Long accountId, int limit, Long categoryId) {
        return (categoryId == null ? movementJpaRepository.findTop50ByAccountIdOrderByMovementDateDescIdDesc(accountId) : movementJpaRepository.findTop50ByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(accountId, categoryId)).stream().limit(limit).map(this::toDomain).toList();
    }

    /**
     * Returns every movement of an account that belongs to a category, newest first.
     *
     * @param accountId  the account identifier
     * @param categoryId the category identifier
     * @return all the matching movements, ordered by movement date and id descending
     */
    public List<Movement> findAllByCategory(Long accountId, Long categoryId) {
        return movementJpaRepository.findByAccountIdAndCategoryIdOrderByMovementDateDescIdDesc(accountId, categoryId).stream().map(this::toDomain).toList();
    }

    /**
     * Finds a movement by its identifier.
     *
     * @param movementId the movement identifier
     * @return the movement, or an empty {@link Optional} if it does not exist
     */
    public Optional<Movement> findById(Long movementId) {
        return movementJpaRepository.findById(movementId).map(this::toDomain);
    }

    /**
     * Counts the movements of an account.
     *
     * @param accountId the account identifier
     * @return the number of movements
     */
    public long count(Long accountId) {
        return movementJpaRepository.countByAccountId(accountId);
    }

    /**
     * Maps a JPA entity to the domain model.
     *
     * @param movementJpa the JPA entity
     * @return the equivalent domain object
     */
    private Movement toDomain(MovementJpa movementJpa) {
        return new Movement(movementJpa.getId(), movementJpa.getAccountId(), movementJpa.getConcept(), movementJpa.getMovementDate(), movementJpa.getAmount(), movementJpa.getAvailableBalance(), movementJpa.getImportStatus(), movementJpa.getCategoryId());
    }
}

/**
 * Outbound adapter that implements the {@link BudgetRepository} domain port using Spring Data JPA.
 * <p>
 * It converts between the domain {@link Budget} and the {@link BudgetJpa} entity.
 */
@Component
class BudgetPersistenceAdapter implements BudgetRepository {
    private final BudgetJpaRepository budgetJpaRepository;

    /**
     * Creates the adapter with the JPA repository it delegates to (injected by Spring).
     *
     * @param budgetJpaRepository the Spring Data repository for budgets
     */
    BudgetPersistenceAdapter(BudgetJpaRepository budgetJpaRepository) {
        this.budgetJpaRepository = budgetJpaRepository;
    }

    /**
     * Inserts or updates a budget (an update happens when the budget already has an id).
     *
     * @param budget the budget to save
     * @return the saved budget, including its generated id
     */
    public Budget save(Budget budget) {
        var savedBudgetJpa = budgetJpaRepository.save(new BudgetJpa(budget.id(), budget.categoryId(), budget.periodStart(), budget.periodEnd(), budget.amount()));
        return new Budget(savedBudgetJpa.getId(), savedBudgetJpa.getCategoryId(), savedBudgetJpa.getPeriodStart(), savedBudgetJpa.getPeriodEnd(), savedBudgetJpa.getAmount());
    }

    /**
     * Lists the budgets defined for a category.
     *
     * @param categoryId the category identifier
     * @return the budgets of the category
     */
    public List<Budget> findByCategory(Long categoryId) {
        return budgetJpaRepository.findByCategoryId(categoryId).stream().map(budgetJpa -> new Budget(budgetJpa.getId(), budgetJpa.getCategoryId(), budgetJpa.getPeriodStart(), budgetJpa.getPeriodEnd(), budgetJpa.getAmount())).toList();
    }
}