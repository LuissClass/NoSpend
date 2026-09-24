package com.onlymymoney.qa;

import com.onlymymoney.adapter.in.csv.OpenCsvMovementReader;
import com.onlymymoney.application.*;
import com.onlymymoney.domain.exception.DomainException;
import com.onlymymoney.domain.model.*;
import com.onlymymoney.domain.port.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * QA tests mapped to the supplied Personal Expense Control QA pack.
 * Tests that require a real browser, Docker/PostgreSQL or visual verification
 * remain manual/e2e checks; these tests cover the deterministic application rules.
 */

// TODO TEST CON DIFERENTES FORMATOS DE FECHA

class OnlyMyMoneyQATest {

    static final Long ACCOUNT = 1L;
    static final BigDecimal ZERO = new BigDecimal("0.00");

    Movement movement(Long id, String concept, String date, String amount, String balance, Long category) {
        return new Movement(id, ACCOUNT, concept, LocalDate.parse(date), new BigDecimal(amount),
                new BigDecimal(balance), ImportStatus.ADDED_TO_MAIN_CSV, category);
    }

    @Test @DisplayName("TC-026 / NFR-02: money arithmetic is exact")
    void monetaryPrecision() {
        assertEquals(new BigDecimal("0.30"), new BigDecimal("0.10").add(new BigDecimal("0.20")));
    }

    @Test @DisplayName("TC-003/004 / FR-03: import lifecycle reaches examining or ignored")
    void importLifecycle() {
        OpenCsvMovementReader reader = new OpenCsvMovementReader();
        String csv = "concepto,fecha,importe,saldo_disponible\nGood,2026-09-19,-34.50,100.00\nBad,not-a-date,-5.00,95.00\n";
        List<Movement> rows = reader.read(in(csv), ACCOUNT);
        assertEquals(2, rows.size());
        assertEquals(ImportStatus.CSV, rows.get(0).status());
        assertEquals(ImportStatus.IGNORED, rows.get(1).status());
    }

    @Test @DisplayName("TC-001 / FR-01: supported CSV is parsed")
    void validCsvIsAccepted() {
        OpenCsvMovementReader reader = new OpenCsvMovementReader();
        String csv = "concepto,fecha,importe,saldo_disponible\nMERCADONA,2026/09/19,-34.50,965.50\n";
        List<Movement> rows = reader.read(in(csv), ACCOUNT);
        assertEquals(1, rows.size());
        assertEquals("MERCADONA", rows.get(0).concept());
        assertEquals(new BigDecimal("-34.50"), rows.get(0).amount());
    }

    @Test
    @DisplayName("TC-002 / FR-01: supported CSV file is parsed")
    void validCsvFileIsAccepted() throws Exception {
        OpenCsvMovementReader reader = new OpenCsvMovementReader();

        try (InputStream in = getClass().getResourceAsStream("/movimientos.csv")) {
            assertNotNull(in, "CSV file not found in resources");

            List<Movement> rows = reader.read(in, ACCOUNT);

            assertEquals(7, rows.size());
            assertEquals("Pago supermercado", rows.get(0).concept());
            assertEquals(new BigDecimal("-23.45"), rows.get(0).amount());
        }
    }

    @Test @DisplayName("TC-002 / FR-02: missing required CSV column is rejected")
    void invalidCsvStructureIsRejected() {
        OpenCsvMovementReader reader = new OpenCsvMovementReader();
        String csv = "concept,date,amount\nMERCADONA,2026-09-19,-34.50\n";
        assertThrows(IllegalArgumentException.class, () -> reader.read(in(csv), ACCOUNT));
    }

    @Test @DisplayName("TC-004 / FR-02: malformed movement is represented as ignored data")
    void malformedMovementBecomesIgnored() {
        OpenCsvMovementReader reader = new OpenCsvMovementReader();
        String csv = "concepto,fecha,importe,saldo_disponible\nMERCADONA,not-a-date,-34.50,965.50\n";
        Movement row = reader.read(in(csv), ACCOUNT).get(0);
        assertEquals(ImportStatus.IGNORED, row.status());
    }

    @Nested
    class ImportTests {
        AccountRepository accounts;
        MovementRepository movements;
        CategoryRepository categories;
        CsvReader csv;
        ImportService service;

        @BeforeEach
        void setUp() {
            accounts = mock(AccountRepository.class);
            movements = mock(MovementRepository.class);
            categories = mock(CategoryRepository.class);
            csv = mock(CsvReader.class);
            service = new ImportService(csv, movements, accounts, categories);
            when(accounts.findById(ACCOUNT)).thenReturn(Optional.of(new Account(ACCOUNT, "Me", Instant.now(), new BigDecimal("100.00"))));
        }

        @Test @DisplayName("TC-005/033 / FR-04: available balance detects duplicates")
        void duplicateByAvailableBalanceIsIgnored() {
            Movement row = movement(null, "A", "2026-09-19", "-10.00", "90.00", null);
            when(csv.read(any(), eq(ACCOUNT))).thenReturn(List.of(row));
            when(movements.existsByAvailableBalance(ACCOUNT, new BigDecimal("90.00"))).thenReturn(true);
            ImportResult result = service.examine(in("ignored"), ACCOUNT);
            assertTrue(result.accepted().isEmpty());
            assertEquals(1, result.ignored().size());
            assertFalse(result.errors().isEmpty());
        }

        @Test @DisplayName("TC-005 / FR-04: duplicate within the same batch is ignored")
        void duplicateWithinBatchIsIgnored() {
            Movement a = movement(null, "A", "2026-09-19", "-10.00", "90.00", null);
            Movement b = movement(null, "B", "2026-09-19", "-5.00", "90.00", null);
            when(csv.read(any(), eq(ACCOUNT))).thenReturn(List.of(a, b));
            when(movements.existsByAvailableBalance(anyLong(), any())).thenReturn(false);
            ImportResult result = service.examine(in("ignored"), ACCOUNT);
            assertEquals(1, result.accepted().size());
            assertEquals(1, result.ignored().size());
        }

        @Test @DisplayName("TC-006 / FR-05: examination does not persist movements")
        void examinationDoesNotPersist() {
            Movement row = movement(null, "A", "2026-09-19", "-10.00", "90.00", null);
            when(csv.read(any(), eq(ACCOUNT))).thenReturn(List.of(row));
            when(movements.existsByAvailableBalance(anyLong(), any())).thenReturn(false);
            service.examine(in("ignored"), ACCOUNT);
            verify(movements, never()).save(any());
        }

        @Test @DisplayName("TC-007/008 / FR-05/06: confirmation persists accepted movements")
        void confirmationPersistsAccepted() {
            Movement row = movement(null, "A", "2026-09-19", "-10.00", "90.00", null);
            ImportResult result = new ImportResult(List.of(row), List.of(), List.of(), new BigDecimal("90.00"));
            Category expenses = new Category(10L, ACCOUNT, "EXPENSES", new BigDecimal("100.00"), true);
            when(categories.findGeneral(ACCOUNT)).thenReturn(Optional.of(expenses));
            service.confirm(result, ACCOUNT);
            verify(movements).save(argThat(m -> m.status() == ImportStatus.ADDED_TO_MAIN_CSV && m.amount().equals(new BigDecimal("-10.00"))));
            verify(accounts).save(argThat(a -> a.totalBalance().equals(new BigDecimal("90.00"))));
            verify(categories).save(argThat(c -> c.generalExpenses() && c.availableBalance().equals(new BigDecimal("90.00"))));
        }

        @Test @DisplayName("TC-015/016 / FR-13: negative and positive movements affect EXPENSES arithmetically")
        void movementAmountsAffectExpenses() {
            Category expenses = new Category(10L, ACCOUNT, "EXPENSES", new BigDecimal("100.00"), true);
            when(categories.findGeneral(ACCOUNT)).thenReturn(Optional.of(expenses));
            Movement expense = movement(null, "expense", "2026-09-19", "-34.50", "65.50", null);
            Movement income = movement(null, "income", "2026-09-20", "150.00", "215.50", null);
            service.confirm(new ImportResult(List.of(expense, income), List.of(), List.of(), new BigDecimal("215.50")), ACCOUNT);
            verify(categories).save(argThat(c -> c.availableBalance().equals(new BigDecimal("215.50"))));
        }
    }

    @Nested
    class CategoryTests {
        CategoryRepository categories;
        CategoryService service;

        @BeforeEach void setUp() { categories = mock(CategoryRepository.class); service = new CategoryService(categories); }

        @Test @DisplayName("TC-012 / FR-10: create category starts at zero")
        void createCategoryStartsAtZero() {
            when(categories.save(any())).thenAnswer(i -> i.getArgument(0));
            Category c = service.create(ACCOUNT, "Saving");
            assertEquals("Saving", c.description());
            assertEquals(0, c.availableBalance().compareTo(ZERO));
            assertFalse(c.generalExpenses());
        }

        @Test @DisplayName("TC-013/018/020 / FR-11/15: transfer updates both categories equally")
        void transferUpdatesBoth() {
            Category expenses = new Category(10L, ACCOUNT, "EXPENSES", new BigDecimal("150.00"), true);
            Category saving = new Category(11L, ACCOUNT, "Saving", new BigDecimal("200.00"), false);
            when(categories.findById(10L)).thenReturn(Optional.of(expenses));
            when(categories.findById(11L)).thenReturn(Optional.of(saving));
            when(categories.findByAccount(ACCOUNT)).thenReturn(List.of(
                    new Category(10L, ACCOUNT, "EXPENSES", new BigDecimal("50.00"), true),
                    new Category(11L, ACCOUNT, "Saving", new BigDecimal("300.00"), false)));
            List<Category> result = service.transfer(ACCOUNT, 10L, 11L, new BigDecimal("100.00"));
            assertEquals(new BigDecimal("50.00"), result.get(0).availableBalance());
            assertEquals(new BigDecimal("300.00"), result.get(1).availableBalance());
        }

        @Test @DisplayName("TC-013: transfer cannot exceed source balance")
        void transferCannotOverdrawSource() {
            when(categories.findById(10L)).thenReturn(Optional.of(new Category(10L, ACCOUNT, "EXPENSES", new BigDecimal("20.00"), true)));
            when(categories.findById(11L)).thenReturn(Optional.of(new Category(11L, ACCOUNT, "Saving", ZERO, false)));
            assertThrows(DomainException.class, () -> service.transfer(ACCOUNT, 10L, 11L, new BigDecimal("100.00")));
        }

        @Test @DisplayName("TC-019/020 / FR-15: negative EXPENSES can be corrected by transfer")
        void negativeExpensesCanBeCorrected() {
            Category expenses = new Category(10L, ACCOUNT, "EXPENSES", new BigDecimal("-50.00"), true);
            Category saving = new Category(11L, ACCOUNT, "Saving", new BigDecimal("200.00"), false);
            when(categories.findById(11L)).thenReturn(Optional.of(saving));
            when(categories.findById(10L)).thenReturn(Optional.of(expenses));
            when(categories.findByAccount(ACCOUNT)).thenReturn(List.of(
                    new Category(10L, ACCOUNT, "EXPENSES", ZERO, true),
                    new Category(11L, ACCOUNT, "Saving", new BigDecimal("150.00"), false)));
            List<Category> result = service.transfer(ACCOUNT, 11L, 10L, new BigDecimal("50.00"));
            assertEquals(0, result.get(0).availableBalance().compareTo(ZERO));
        }
    }

    @Test @DisplayName("TC-014 / FR-12: new accounts receive EXPENSES")
    void newAccountGetsExpenses() {
        AccountRepository accounts = mock(AccountRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        when(accounts.save(any())).thenAnswer(i -> new Account(1L, i.<Account>getArgument(0).holder(), Instant.now(), ZERO));
        when(categories.save(any())).thenAnswer(i -> i.getArgument(0));
        Account a = new AccountService(accounts, categories).create("Me");
        assertNotNull(a);
        verify(categories).save(argThat(c -> c.generalExpenses() && c.description().equals("EXPENSES") && c.availableBalance().compareTo(ZERO) == 0));
    }

    @Test @DisplayName("TC-017 / FR-14: non-general category is untouched by import")
    void nonGeneralCategoryIsNotChangedByImport() {
        AccountRepository accounts = mock(AccountRepository.class);
        MovementRepository movements = mock(MovementRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        CsvReader csv = mock(CsvReader.class);
        when(accounts.findById(ACCOUNT)).thenReturn(Optional.of(new Account(ACCOUNT, "Me", Instant.now(), new BigDecimal("100"))));
        when(csv.read(any(), eq(ACCOUNT))).thenReturn(List.of(movement(null, "A", "2026-09-19", "-10", "90", null)));
        when(movements.existsByAvailableBalance(anyLong(), any())).thenReturn(false);
        when(categories.findGeneral(ACCOUNT)).thenReturn(Optional.of(new Category(1L, ACCOUNT, "EXPENSES", new BigDecimal("100"), true)));
        ImportService s = new ImportService(csv, movements, accounts, categories);
        s.confirm(s.examine(in("x"), ACCOUNT), ACCOUNT);
        verify(categories, never()).save(argThat(c -> !c.generalExpenses()));
    }

    @Test @DisplayName("TC-029 / NFR-05: categorization preserves transaction fields")
    void categorizationPreservesTransactionData() {
        MovementRepository movements = mock(MovementRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        Movement original = movement(7L, "MERCADONA", "2026-09-19", "-34.50", "965.50", null);
        when(movements.findById(7L)).thenReturn(Optional.of(original));
        when(categories.findById(9L)).thenReturn(Optional.of(new Category(9L, ACCOUNT, "Food", ZERO, false)));
        when(movements.save(any())).thenAnswer(i -> i.getArgument(0));
        Movement updated = new MovementService(movements, categories).assignCategory(ACCOUNT, 7L, 9L);
        assertEquals(original.concept(), updated.concept());
        assertEquals(original.date(), updated.date());
        assertEquals(original.amount(), updated.amount());
        assertEquals(original.availableBalance(), updated.availableBalance());
        assertEquals(9L, updated.categoryId());
    }

    @Test @DisplayName("TC-022 / FR-17: accumulated expenses use absolute negative amounts per category")
    void accumulatedExpensesByCategory() {
        MovementRepository movements = mock(MovementRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        Category food = new Category(10L, ACCOUNT, "Food", ZERO, false);
        Category transport = new Category(11L, ACCOUNT, "Transport", ZERO, false);
        when(categories.findByAccount(ACCOUNT)).thenReturn(List.of(food, transport));
        when(movements.findAllByCategory(ACCOUNT, 10L)).thenReturn(List.of(
                movement(1L,"a","2026-09-01","-20","980",10L),
                movement(2L,"b","2026-09-02","-30","950",10L)));
        when(movements.findAllByCategory(ACCOUNT, 11L)).thenReturn(List.of(
                movement(3L,"c","2026-09-03","-10","940",11L)));
        Map<String,BigDecimal> result = new ReportService(movements,categories).expenseByCategory(ACCOUNT);
        assertEquals(new BigDecimal("50"), result.get("Food"));
        assertEquals(new BigDecimal("10"), result.get("Transport"));
    }

    @Test @DisplayName("TC-023 / FR-18: budget validates category ownership, period and amount")
    void budgetRules() {
        BudgetRepository budgets = mock(BudgetRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        when(categories.findById(10L)).thenReturn(Optional.of(new Category(10L, ACCOUNT, "Saving", ZERO, false)));
        when(budgets.save(any())).thenAnswer(i -> i.getArgument(0));
        BudgetService service = new BudgetService(budgets,categories);
        Budget b = service.create(ACCOUNT,10L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),new BigDecimal("300"));
        assertEquals(new BigDecimal("300"),b.amount());
        assertThrows(DomainException.class, () -> service.create(ACCOUNT,10L,LocalDate.of(2026,10,1),LocalDate.of(2026,9,30),new BigDecimal("300")));
        assertThrows(DomainException.class, () -> service.create(ACCOUNT,10L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),new BigDecimal("-1")));
    }

    @Test @DisplayName("TC-024 / FR-19: account service has no numeric account limit")
    void multipleAccountsAreAllowed() {
        AccountRepository accounts = mock(AccountRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        when(accounts.save(any())).thenAnswer(i -> new Account(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE, i.<Account>getArgument(0).holder(), Instant.now(), ZERO));
        when(categories.save(any())).thenAnswer(i -> i.getArgument(0));
        AccountService service = new AccountService(accounts,categories);
        assertNotNull(service.create("A"));
        assertNotNull(service.create("B"));
        assertNotNull(service.create("C"));
        verify(accounts,times(3)).save(any());
    }

    @Test @DisplayName("TC-030/031 / NFR-06: TOTAL MONEY changes on confirm/import, not category operations")
    void totalMoneyOnlyChangesDuringImport() {
        AccountRepository accounts = mock(AccountRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        MovementRepository movements = mock(MovementRepository.class);
        CsvReader csv = mock(CsvReader.class);
        when(accounts.findById(ACCOUNT)).thenReturn(Optional.of(new Account(ACCOUNT,"Me",Instant.now(),new BigDecimal("2000"))));
        when(csv.read(any(),eq(ACCOUNT))).thenReturn(List.of(movement(null,"income","2026-09-20","100","2100",null)));
        when(movements.existsByAvailableBalance(anyLong(),any())).thenReturn(false);
        when(categories.findGeneral(ACCOUNT)).thenReturn(Optional.of(new Category(10L,ACCOUNT,"EXPENSES",new BigDecimal("2000"),true)));
        ImportService service = new ImportService(csv,movements,accounts,categories);
        service.confirm(service.examine(in("x"),ACCOUNT),ACCOUNT);
        verify(accounts).save(argThat(a -> a.totalBalance().equals(new BigDecimal("2100"))));
    }

    @Test @DisplayName("TC-027 / NFR-03: repeated same CSV is idempotent at movement level")
    void repeatedCsvDoesNotCreateDuplicate() {
        AccountRepository accounts = mock(AccountRepository.class);
        MovementRepository movements = mock(MovementRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        CsvReader csv = mock(CsvReader.class);
        when(accounts.findById(ACCOUNT)).thenReturn(Optional.of(new Account(ACCOUNT,"Me",Instant.now(),new BigDecimal("100"))));
        Movement row = movement(null,"A","2026-09-19","-10","90",null);
        when(csv.read(any(),eq(ACCOUNT))).thenReturn(List.of(row));
        when(movements.existsByAvailableBalance(ACCOUNT,new BigDecimal("90"))).thenReturn(false, true);
        when(categories.findGeneral(ACCOUNT)).thenReturn(Optional.of(new Category(10L,ACCOUNT,"EXPENSES",new BigDecimal("100"),true)));
        ImportService service = new ImportService(csv,movements,accounts,categories);
        service.confirm(service.examine(in("x"),ACCOUNT),ACCOUNT);
        ImportResult second = service.examine(in("x"),ACCOUNT);
        assertTrue(second.accepted().isEmpty());
        assertEquals(1,second.ignored().size());
    }

    private static ByteArrayInputStream in(String s) { return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)); }
}
