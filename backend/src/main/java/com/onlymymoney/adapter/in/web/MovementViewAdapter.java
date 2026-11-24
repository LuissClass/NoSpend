package com.onlymymoney.adapter.in.web;

import com.onlymymoney.domain.model.Movement;
import com.onlymymoney.domain.port.MovementRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Adapter that implements the web layer's {@link MovementRepositoryView} by delegating
 * to the domain {@link MovementRepository} port.
 * <p>
 * It keeps the controller decoupled from the domain repository, exposing only the read operation it needs.
 */
@Component
public class MovementViewAdapter implements MovementRepositoryView {
    private final MovementRepository r;

    /**
     * Creates the adapter with the repository it delegates to (injected by Spring).
     *
     * @param r the domain repository used to read movements
     */
    public MovementViewAdapter(MovementRepository r) {
        this.r = r;
    }

    /**
     * Returns the most recent movements of an account, delegating to the repository.
     *
     * @param id         the account identifier
     * @param limit      maximum number of movements to return
     * @param categoryId optional category filter; may be {@code null} to skip filtering
     * @return the latest movements matching the criteria
     */
    public List<Movement> latest(Long id, int limit, Long categoryId) {
        return r.findLatest(id, limit, categoryId);
    }
}