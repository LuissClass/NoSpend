package com.onlymymoney.application;

import com.onlymymoney.domain.model.Movement;

import java.util.*;

/**
 * Result of examining a CSV import.
 * <p>
 * It is returned by the examine step so the user can review it, and it is sent back
 * unchanged to the confirm step to actually persist the accepted movements.
 *
 * @param accepted     movements that passed validation and will be saved when the import is confirmed
 * @param ignored      movements that were discarded, either because they are invalid or duplicated
 * @param errors       human-readable messages explaining why movements were ignored
 * @param totalBalance the account balance that will be stored once the import is confirmed
 */
public record ImportResult(List<Movement> accepted, List<Movement> ignored, List<String> errors,
                           java.math.BigDecimal totalBalance) {
}