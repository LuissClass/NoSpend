package com.onlymymoney.domain.port;

import com.onlymymoney.domain.model.Movement;

import java.io.InputStream;
import java.util.*;

/**
 * Domain port for reading movements from a CSV source.
 * It is implemented by an inbound adapter such as {@code OpenCsvMovementReader}.
 */
public interface CsvReader {
    /**
     * Reads the movements contained in a CSV stream.
     *
     * @param input     the CSV content
     * @param accountId the account the movements will belong to
     * @return the movements read from the stream
     */
    List<Movement> read(InputStream input, Long accountId);
}