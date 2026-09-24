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

@Component
public class OpenCsvMovementReader implements CsvReader {
    public List<Movement> read(InputStream input, Long accountId) {
        try {
            String content = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            try (Reader reader = new StringReader(content)) {
                char sep = content.lines().findFirst().map(line -> line.chars().filter(c -> c == ',' || c == ';' || c == '\t').count()).orElse(1L) > 1 ? (content.lines().findFirst().get().contains(";") ? ';' : content.lines().findFirst().get().contains("\t") ? '\t' : ',') : ',';
                CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(new CSVParserBuilder().withSeparator(sep).build()).build();
                List<String[]> rows = csv.readAll();
                if (rows.isEmpty()) throw new IllegalArgumentException("CSV is empty");
                Set<String> required = Set.of(
                        "Concepto", "Fecha", "Importe", "Saldo_disponible"
                        //"concept", "date", "amount", "available_balance"
                );
                String[] h = findHeader(rows, required);
                int headerIndex = rows.indexOf(h);
                List<String[]> dataRows = rows.subList(headerIndex + 1, rows.size());
                Map<String, Integer> idx = new HashMap<>();
                for (int i = 0; i < h.length; i++) idx.put(norm(h[i]), i);
                int concept = col(idx, "concept", "concepto", "description", "descripcion", "merchant", "comercio", "name");
                int date = col(idx, "date", "fecha", "movement_date", "transaction_date");
                int amount = col(idx, "amount", "importe", "valor", "transaction_amount");
                int balance = col(idx, "available_balance", "avaliable_balance", "saldo_disponible", "saldo", "balance");
                List<Movement> out = new ArrayList<>();
                for (int i = 0; i < dataRows.size(); i++) {
                    String[] r = dataRows.get(i);
                    if (r.length == 0) continue;
                    try {
                        out.add(new Movement(null, accountId, cell(r, concept), parseDate(cell(r, date)), parseMoney(cell(r, amount)), parseMoney(cell(r, balance)), ImportStatus.CSV, null));
                    } catch (Exception e) { // TODO AGREGAR MÁS EXCEPCIONES
                        out.add(new Movement(null, accountId, cell(r, concept), null, null, null, ImportStatus.IGNORED, null));
                    }
                }
                return out;
            }
        } catch (Exception e) {
            // TODO EL PUTO RATO TIRA ESTO????
            throw new IllegalArgumentException("Unable to parse CSV: " + e.getMessage(), e);
        }
    }

    private int col(Map<String, Integer> m, String... names) {
        for (String n : names) if (m.containsKey(norm(n))) return m.get(norm(n));
        throw new IllegalArgumentException("Missing required CSV column: " + Arrays.toString(names));
    }

    private String cell(String[] r, int i) {
        return i < r.length ? r[i].trim() : null;
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replace("\uFEFF", "").replace(" ", "_");
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) return null;
        return new BigDecimal(normalizarEuro(s));
    }

    private LocalDate parseDate(String s) {
        for (DateTimeFormatter f : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,                     // yyyy-MM-dd
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),            // dd/MM/yyyy
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),            // dd-MM-yyyy
                DateTimeFormatter.ofPattern("yyyy/MM/dd")             // yyyy/MM/dd
        )) {
            try {
                return LocalDate.parse(s, f);
            } catch (Exception ignored) { // TODO Exception ignored
            }
        }
        throw new IllegalArgumentException("Unsupported date: " + s);
    }


    private String[] findHeader(List<String[]> rows, Set<String> requiredColumns) {
        for (String[] row : rows) {
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < row.length; i++) {
                idx.put(norm(row[i]), i);
            }

            boolean ok = requiredColumns.stream().allMatch(c -> idx.containsKey(norm(c)));
            if (ok) {
                return row;
            }
        }

        throw new IllegalArgumentException("No valid header found in CSV");
    }

    public static String normalizarEuro(String valor) {
        if (valor == null) return null;

        // Verifica si contiene EUR antes de transformar
        if (valor.contains("EUR")) {
            String limpio = valor.replace("EUR", "").trim();
            limpio = limpio.replace(",", ".");
            return limpio;
        }

        // Si no contiene EUR, se devuelve sin tocar
        return valor;
    }

}
