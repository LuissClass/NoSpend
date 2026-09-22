# Requirements implementation map

The implementation follows the supplied notes and QA pack.

| Requirement | Implementation |
|---|---|
| FR-01/02/03 | OpenCSV reader + ImportService |
| FR-04 | available-balance duplicate check |
| FR-05/06 | Import preview + confirm endpoint |
| FR-07/09 | movement endpoint, latest 50 and arbitrary capped query for latest 10 |
| FR-08 | categoryId filter |
| FR-10 | category creation |
| FR-11 | manual transfer |
| FR-12 | Flyway seeds EXPENSES for initial account and AccountService creates it for new accounts |
| FR-13/14/15 | ImportService updates EXPENSES only; manual transfer handles correction |
| FR-16 | dashboard categories + TOTAL MONEY |
| FR-17 | accumulated expenses report + Recharts |
| FR-18 | budget schema/port prepared |
| FR-19 | account creation |
| NFR-01..07 | local Docker, BigDecimal, idempotent duplicate check, local DB, immutable movement fields, import-only total balance, no internal-movement trace |

## Source-derived caveats

The supplied source documents contain a contradiction around whether a movement can belong to a category. The QA pack requires category filtering and expense accumulation, so category assignment is implemented as separate mutable metadata rather than altering immutable transaction data.

The current duplicate criterion is intentionally available balance, even though this can be broader than a typical bank-transaction identity key.
