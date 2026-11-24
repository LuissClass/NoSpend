package com.onlymymoney.adapter.in.web;

import com.onlymymoney.application.*;
import com.onlymymoney.domain.model.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * REST controller that exposes the application's HTTP API under the {@code /api} prefix.
 * <p>
 * It is an inbound adapter: it only translates HTTP requests into calls to the application
 * services (accounts, categories, movements, imports, budgets and reports) and returns their results.
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final AccountService accounts;
    private final CategoryService cats;
    private final ImportService imports;
    private final MovementRepositoryView movementView;
    private final MovementService movementService;
    private final ReportService reports;
    private final BudgetService budgets;

    /**
     * Creates the controller with all the collaborators it needs (injected by Spring).
     *
     * @param a  service that manages accounts
     * @param c  service that manages categories
     * @param i  service that handles movement imports
     * @param mv read-only view used to query the latest movements
     * @param ms service that manages movements
     * @param r  service that builds reports
     * @param bs service that manages budgets
     */
    public ApiController(AccountService a, CategoryService c, ImportService i, MovementRepositoryView mv, MovementService ms, ReportService r, BudgetService bs) {
        accounts = a;
        cats = c;
        imports = i;
        movementView = mv;
        movementService = ms;
        reports = r;
        budgets = bs;
    }

    /**
     * {@code GET /api/accounts}: lists all accounts.
     *
     * @return every existing account
     */
    @GetMapping("/accounts")
    List<Account> accounts() {
        return accounts.all();
    }

    /**
     * {@code POST /api/accounts}: creates a new account.
     *
     * @param body request body containing the account holder
     * @return the created account
     */
    @PostMapping("/accounts")
    Account createAccount(@RequestBody CreateAccount body) {
        return accounts.create(body.holder());
    }

    /**
     * {@code GET /api/accounts/{id}}: retrieves a single account.
     *
     * @param id the account identifier
     * @return the requested account
     */
    @GetMapping("/accounts/{id}")
    Account account(@PathVariable Long id) {
        return accounts.get(id);
    }

    /**
     * {@code GET /api/accounts/{id}/categories}: lists the categories of an account.
     *
     * @param id the account identifier
     * @return the categories that belong to the account
     */
    @GetMapping("/accounts/{id}/categories")
    List<Category> categories(@PathVariable Long id) {
        return cats.list(id);
    }

    /**
     * {@code POST /api/accounts/{id}/categories}: creates a category in an account.
     *
     * @param id   the account identifier
     * @param body request body containing the category description
     * @return the created category
     */
    @PostMapping("/accounts/{id}/categories")
    Category createCategory(@PathVariable Long id, @RequestBody CreateCategory body) {
        return cats.create(id, body.description());
    }

    /**
     * {@code POST /api/accounts/{id}/categories/transfer}: moves an amount from one category to another.
     *
     * @param id   the account identifier
     * @param body request body with the source category, the destination category and the amount
     * @return the resulting list of categories
     */
    @PostMapping("/accounts/{id}/categories/transfer")
    List<Category> transfer(@PathVariable Long id, @RequestBody Transfer body) {
        return cats.transfer(id, body.fromCategoryId(), body.toCategoryId(), body.amount());
    }

    /**
     * {@code PATCH /api/accounts/{accountId}/movements/{movementId}/category}: assigns a category to a movement.
     *
     * @param accountId  the account identifier
     * @param movementId the movement identifier
     * @param body       request body containing the category identifier
     * @return the updated movement
     */
    @PatchMapping("/accounts/{accountId}/movements/{movementId}/category")
    Movement assignCategory(@PathVariable Long accountId, @PathVariable Long movementId, @RequestBody AssignCategory body) {
        return movementService.assignCategory(accountId, movementId, body.categoryId());
    }

    /**
     * {@code POST /api/accounts/{accountId}/budgets}: creates a budget for a category over a period.
     *
     * @param accountId the account identifier
     * @param b         request body with the category, the period start and end dates, and the amount
     * @return the created budget
     */
    @PostMapping("/accounts/{accountId}/budgets")
    Budget createBudget(@PathVariable Long accountId, @RequestBody CreateBudget b) {
        return budgets.create(accountId, b.categoryId(), b.periodStart(), b.periodEnd(), b.amount());
    }

    /**
     * {@code GET /api/categories/{categoryId}/budgets}: lists the budgets of a category.
     *
     * @param categoryId the category identifier
     * @return the budgets defined for the category
     */
    @GetMapping("/categories/{categoryId}/budgets")
    List<Budget> budgets(@PathVariable Long categoryId) {
        return budgets.list(categoryId);
    }

    /**
     * {@code GET /api/accounts/{id}/movements}: lists the latest movements of an account.
     * <p>
     * The {@code limit} parameter is clamped to the range 1 to 50, so values outside it are silently adjusted.
     *
     * @param id         the account identifier
     * @param limit      maximum number of movements to return (default 50, clamped between 1 and 50)
     * @param categoryId optional category filter; when {@code null}, movements of any category are returned
     * @return the latest movements matching the criteria
     */
    @GetMapping("/accounts/{id}/movements")
    List<Movement> movements(@PathVariable Long id, @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) Long categoryId) {
        return movementView.latest(id, Math.min(Math.max(limit, 1), 50), categoryId);
    }

    /**
     * {@code POST /api/accounts/{id}/imports}: first step of the import flow.
     * <p>
     * Receives a CSV file as multipart form data and examines it, returning the result of the analysis.
     *
     * @param id   the account identifier
     * @param file the uploaded file, sent in the {@code file} part
     * @return the result of examining the file, which can then be sent to the confirm endpoint
     * @throws Exception if the uploaded file cannot be read
     */
    @PostMapping(value = "/accounts/{id}/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImportResult examine(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws Exception {
        return imports.examine(file.getInputStream(), id);
    }

    /**
     * {@code POST /api/accounts/{id}/imports/confirm}: second step of the import flow.
     * <p>
     * Confirms a previously examined import result.
     *
     * @param id     the account identifier
     * @param result the import result to confirm
     * @return the result after the confirmation
     */
    @PostMapping("/accounts/{id}/imports/confirm")
    ImportResult confirm(@PathVariable Long id, @RequestBody ImportResult result) {
        return imports.confirm(result, id);
    }

    /**
     * {@code GET /api/accounts/{id}/reports/expenses}: expense report grouped by category.
     *
     * @param id the account identifier
     * @return a map whose keys are category names and whose values are the total expenses of each one
     */
    @GetMapping("/accounts/{id}/reports/expenses")
    Map<String, BigDecimal> expenses(@PathVariable Long id) {
        return reports.expenseByCategory(id);
    }

    /**
     * Request body used to create a budget.
     *
     * @param categoryId  the category the budget applies to (required)
     * @param periodStart first day of the budget period (required)
     * @param periodEnd   last day of the budget period (required)
     * @param amount      budgeted amount, must be greater than or equal to 0.00 (required)
     */
    public record CreateBudget(@NotNull Long categoryId, @NotNull java.time.LocalDate periodStart,
                               @NotNull java.time.LocalDate periodEnd, @NotNull @DecimalMin("0.00") BigDecimal amount) {
    }

    /**
     * Request body used to assign a category to a movement.
     *
     * @param categoryId the category to assign (required)
     */
    public record AssignCategory(@NotNull Long categoryId) {
    }

    /**
     * Request body used to create an account.
     *
     * @param holder the name of the account holder (required, must not be blank)
     */
    public record CreateAccount(@NotBlank String holder) {
    }

    /**
     * Request body used to create a category.
     *
     * @param description the category description (required, must not be blank)
     */
    public record CreateCategory(@NotBlank String description) {
    }

    /**
     * Request body used to transfer an amount between two categories.
     *
     * @param fromCategoryId the category the amount is taken from (required)
     * @param toCategoryId   the category the amount is moved to (required)
     * @param amount         the amount to transfer, must be at least 0.01 (required)
     */
    public record Transfer(@NotNull Long fromCategoryId, @NotNull Long toCategoryId,
                           @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }
}

/**
 * Read-only view over stored movements, used by the web layer to query the latest ones.
 * <p>
 * It is implemented by {@code MovementViewAdapter}, which delegates to the domain repository.
 */
interface MovementRepositoryView {
    /**
     * Returns the most recent movements of an account.
     *
     * @param id         the account identifier
     * @param limit      maximum number of movements to return
     * @param categoryId optional category filter; may be {@code null} to skip filtering
     * @return the latest movements matching the criteria
     */
    List<Movement> latest(Long id, int limit, Long categoryId);
}