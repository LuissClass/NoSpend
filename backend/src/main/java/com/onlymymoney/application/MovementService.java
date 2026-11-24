package com.onlymymoney.application;

import com.onlymymoney.domain.exception.DomainException;
import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*;
import org.springframework.stereotype.Service;

/**
 * Application service that manages operations on individual movements.
 */
@Service
public class MovementService {
    private final MovementRepository movements;
    private final CategoryRepository categories;

    /**
     * Creates the service with its collaborators (injected by Spring).
     *
     * @param m the movement repository
     * @param c the category repository
     */
    public MovementService(MovementRepository m, CategoryRepository c) {
        movements = m;
        categories = c;
    }

    /**
     * Assigns a category to a movement.
     * <p>
     * Both the movement and the category must exist and belong to the given account.
     * Every other field of the movement is kept as it was.
     *
     * @param accountId  the account the movement and the category must belong to
     * @param movementId the movement to update
     * @param categoryId the category to assign
     * @return the updated movement
     * @throws DomainException if the movement or category does not exist, or if either does not belong to the account
     */
    public Movement assignCategory(Long accountId, Long movementId, Long categoryId) {
        Movement m = movements.findById(movementId).orElseThrow(() -> new DomainException("Movement not found"));
        Category c = categories.findById(categoryId).orElseThrow(() -> new DomainException("Category not found"));
        // Prevent assigning resources that belong to a different account.
        if (!m.accountId().equals(accountId) || !c.accountId().equals(accountId))
            throw new DomainException("Resource does not belong to account");
        // Save a copy of the movement with only the category changed.
        return movements.save(new Movement(m.id(), m.accountId(), m.concept(), m.date(), m.amount(), m.availableBalance(), m.status(), categoryId));
    }
}