package com.onlymymoney.adapter.in.csv;

import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.CsvReader;
import com.opencsv.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Inbound adapter that reads bank movements from a CSV file using the OpenCSV library.
 * <p>
 * It implements the {@link CsvReader} domain port. The reader is tolerant to different
 * CSV flavours: it auto-detects the column separator, skips any lines found before the
 * header, resolves columns by several accepted names (English and Spanish), and supports
 * multiple date formats and Euro-formatted amounts.
 */
@Component
public class OpenCsvMovementReader implements CsvReader {

    /**
     * Parses a CSV stream into a list of {@link Movement} objects for the given account.
     * <p>
     * Rows that can be read correctly are returned with status {@code ImportStatus.CSV}.
     * Rows that fail to parse are not discarded: they are returned with status
     * {@code ImportStatus.IGNORED} and only their concept filled in.
     *
     * @param input     the CSV content as a stream (decoded as UTF-8)
     * @param accountId the identifier of the account the movements belong to
     * @return the list of parsed movements, in the same order as in the file
     * @throws IllegalArgumentException if the CSV is empty, has no valid header, lacks a
     *                                  required column, or cannot be parsed for any other reason
     */
    public List<Movement> read(InputStream input, Long accountId) {
        try {
            // Read the whole stream into memory so the first line can be inspected to detect the separator.
            String content = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            try (Reader reader = new StringReader(content)) {
                // Separator detection based on the first line: if it contains more than one separator
                // character (',' ';' or tab), pick ';' first, then tab, then ','. Otherwise default to ','.
                char separator = content.lines().findFirst().map(line -> line.chars().filter(character -> character == ',' || character == ';' || character == '\t').count()).orElse(1L) > 1 ? (content.lines().findFirst().get().contains(";") ? ';' : content.lines().findFirst().get().contains("\t") ? '\t' : ',') : ',';
                CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(new CSVParserBuilder().withSeparator(separator).build()).build();
                List<String[]> rows = csvReader.readAll();
                if (rows.isEmpty()) throw new IllegalArgumentException("CSV is empty");
                // Columns that must be present in the header row for it to be considered valid.
                Set<String> requiredColumns = Set.of(
                        "Concepto", "Fecha", "Importe", "Saldo_disponible"
                        //"concept", "date", "amount", "available_balance"
                );
                // Locate the header row (some bank exports include extra lines before it).
                String[] headerRow = findHeader(rows, requiredColumns);
                // indexOf relies on reference equality here, since findHeader returns the very same array instance.
                int headerIndex = rows.indexOf(headerRow);
                // Everything after the header row is considered data.
                List<String[]> dataRows = rows.subList(headerIndex + 1, rows.size());
                // Map each normalized header name to its column position.
                Map<String, Integer> columnIndexByName = new HashMap<>();
                for (int columnIndex = 0; columnIndex < headerRow.length; columnIndex++) columnIndexByName.put(normalizeHeaderName(headerRow[columnIndex]), columnIndex);
                // Resolve the position of each field, accepting several alternative column names.
                int conceptColumn = findColumnIndex(columnIndexByName, "concept", "concepto", "description", "descripcion", "merchant", "comercio", "name");
                int dateColumn = findColumnIndex(columnIndexByName, "date", "fecha", "movement_date", "transaction_date");
                int amountColumn = findColumnIndex(columnIndexByName, "amount", "importe", "valor", "transaction_amount");
                int balanceColumn = findColumnIndex(columnIndexByName, "available_balance", "avaliable_balance", "saldo_disponible", "saldo", "balance");
                List<Movement> movements = new ArrayList<>();
                for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++) {
                    String[] row = dataRows.get(rowIndex);
                    // Skip rows with no cells at all.
                    if (row.length == 0) continue;
                    try {
                        // Valid row: id is null (not persisted yet) and the last argument is left null (the category id, not assigned yet).
                        movements.add(new Movement(null, accountId, readCell(row, conceptColumn), parseDate(readCell(row, dateColumn)), parseMoney(readCell(row, amountColumn)), parseMoney(readCell(row, balanceColumn)), ImportStatus.CSV, null));
                    } catch (Exception exception) {
                        // Unparseable row: keep only the concept and flag it as IGNORED so it can be reviewed later.
                        movements.add(new Movement(null, accountId, readCell(row, conceptColumn), null, null, null, ImportStatus.IGNORED, null));
                    }
                }
                return movements;
            }
        } catch (Exception exception) {
            // Any failure (I/O, parsing, missing header or column) is wrapped in an IllegalArgumentException.
            throw new IllegalArgumentException("Unable to parse CSV: " + exception.getMessage(), exception);
        }
    }

    /**
     * Finds the position of a column given a list of accepted names.
     * <p>
     * Names are normalized before the lookup, and the first one found wins.
     *
     * @param columnIndexByName map of normalized header names to column indexes
     * @param acceptedNames     accepted alternative names for the column, in order of priority
     * @return the zero-based column index
     * @throws IllegalArgumentException if none of the names is present in the header
     */
    private int findColumnIndex(Map<String, Integer> columnIndexByName, String... acceptedNames) {
        for (String name : acceptedNames) if (columnIndexByName.containsKey(normalizeHeaderName(name))) return columnIndexByName.get(normalizeHeaderName(name));
        throw new IllegalArgumentException("Missing required CSV column: " + Arrays.toString(acceptedNames));
    }

    /**
     * Safely reads a cell from a row.
     *
     * @param row         the row
     * @param columnIndex the zero-based column index
     * @return the trimmed cell value, or {@code null} if the row is shorter than the requested index
     */
    private String readCell(String[] row, int columnIndex) {
        return columnIndex < row.length ? row[columnIndex].trim() : null;
    }

    /**
     * Normalizes a header name so that different spellings can be compared:
     * trims it, lowercases it, removes the UTF-8 BOM character and replaces spaces with underscores.
     *
     * @param headerName the raw header name (may be {@code null})
     * @return the normalized name, or an empty string if {@code headerName} is {@code null}
     */
    private String normalizeHeaderName(String headerName) {
        return headerName == null ? "" : headerName.trim().toLowerCase(Locale.ROOT).replace("\uFEFF", "").replace(" ", "_");
    }

    /**
     * Converts a textual amount into a {@link BigDecimal}, after normalizing Euro formatting
     * through {@link #normalizeEuro(String)}.
     *
     * @param rawAmount the raw amount text
     * @return the parsed amount, or {@code null} if the text is {@code null} or blank
     * @throws NumberFormatException if the text is not a valid number after normalization
     */
    private BigDecimal parseMoney(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) return null;
        return new BigDecimal(normalizeEuro(rawAmount));
    }

    /**
     * Parses a date trying several supported formats, in order, until one succeeds.
     * <p>
     * Supported formats: {@code yyyy-MM-dd}, {@code dd/MM/yyyy}, {@code dd-MM-yyyy} and {@code yyyy/MM/dd}.
     *
     * @param rawDate the raw date text
     * @return the parsed date
     * @throws IllegalArgumentException if no supported format matches
     */
    private LocalDate parseDate(String rawDate) {
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,                     // yyyy-MM-dd
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )) {
            try {
                return LocalDate.parse(rawDate, formatter);
            } catch (Exception ignored) {
                // This format did not match; try the next one.
            }
        }
        throw new IllegalArgumentException("Unsupported date: " + rawDate);
    }


    /**
     * Looks for the header row: the first row that contains all the required columns.
     * <p>
     * Column names are compared after normalization, so case, spaces and BOM characters are ignored.
     *
     * @param rows            all the rows read from the CSV
     * @param requiredColumns the column names the header must contain
     * @return the header row
     * @throws IllegalArgumentException if no row contains all the required columns
     */
    private String[] findHeader(List<String[]> rows, Set<String> requiredColumns) {
        for (String[] row : rows) {
            // Index the normalized cells of this candidate row.
            Map<String, Integer> columnIndexByName = new HashMap<>();
            for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
                columnIndexByName.put(normalizeHeaderName(row[columnIndex]), columnIndex);
            }

            // The row is the header only if every required column is present.
            boolean hasAllRequiredColumns = requiredColumns.stream().allMatch(requiredColumn -> columnIndexByName.containsKey(normalizeHeaderName(requiredColumn)));
            if (hasAllRequiredColumns) {
                return row;
            }
        }

        throw new IllegalArgumentException("No valid header found in CSV");
    }

    /**
     * Normalizes an amount written in Euro format so it can be parsed as a number.
     * <p>
     * If the value contains the text "EUR", that text is removed, the value is trimmed and the
     * decimal comma is replaced by a dot. Values without "EUR" are returned unchanged.
     *
     * @param value the raw amount text (may be {@code null})
     * @return the normalized text, or {@code null} if the input is {@code null}
     */
    public static String normalizeEuro(String value) {
        if (value == null) return null;

        // Checks whether the value contains EUR before transforming it
        if (value.contains("EUR")) {
            String cleaned = value.replace("EUR", "").trim();
            cleaned = cleaned.replace(",", ".");
            return cleaned;
        }

        // If it does not contain EUR, the value is returned untouched
        return value;
    }

}