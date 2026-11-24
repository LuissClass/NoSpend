package com.onlymymoney.application;

import com.onlymymoney.domain.port.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Application service that builds reports from the stored movements.
 */
@Service
public class ReportService {
    private final MovementRepository movements;
    private final CategoryRepository cats;

    /**
     * Creates the service with its collaborators (injected by Spring).
     *
     * @param m the movement repository
     * @param c the category repository
     */
    public ReportService(MovementRepository m, CategoryRepository c) {
        movements = m;
        cats = c;
    }

    /**
     * Calculates the total expenses of each category of an account.
     * <p>
     * Only movements with a negative amount are considered expenses, and they are added up
     * as positive values. Categories without expenses appear with a total of zero. Movements
     * that have no category are not included in any total.
     * <p>
     * The map keys are category descriptions, so two categories sharing the same description
     * would end up merged into a single entry (the last one processed wins).
     *
     * @param accountId the account identifier
     * @return a map from category description to total expenses, in the order the categories are returned by the repository
     */
    public Map<String, BigDecimal> expenseByCategory(Long accountId) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        // Initialize every category with zero so empty categories are also reported.
        for (var c : cats.findByAccount(accountId)) out.put(c.description(), BigDecimal.ZERO);
        // Transaction category metadata is optional; uncategorized expenses are intentionally not assigned.
        for (var c : cats.findByAccount(accountId)) {
            // Sum the absolute value of the negative amounts (expenses) of this category.
            BigDecimal sum = movements.findAllByCategory(accountId, c.id()).stream().filter(m -> m.amount().signum() < 0).map(m -> m.amount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.put(c.description(), sum);
        }
        return out;
    }
}