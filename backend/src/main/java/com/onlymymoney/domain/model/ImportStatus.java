package com.onlymymoney.domain.model;

/**
 * Status of a movement along the CSV import flow.
 * <ul>
 *     <li>{@code CSV}: the row was read from the CSV file and parsed correctly.</li>
 *     <li>{@code IMPORTING}: the movement is being imported (not used by the code shown so far).</li>
 *     <li>{@code EXAMINING}: the movement passed validation during the examine step and awaits confirmation.</li>
 *     <li>{@code ADDED_TO_MAIN_CSV}: the movement was confirmed and saved as a definitive one.</li>
 *     <li>{@code IGNORED}: the movement was discarded because it is invalid, duplicated or could not be parsed.</li>
 * </ul>
 */
public enum ImportStatus {CSV, IMPORTING, EXAMINING, ADDED_TO_MAIN_CSV, IGNORED}