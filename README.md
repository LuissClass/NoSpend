# Only My Money

Local personal expense-control MVP based directly on the supplied project notes and QA pack.

## Stack
- Java 21
- Spring Boot 3.5.5
- Spring Web / Spring Data JPA
- OpenCSV 5.12
- Flyway
- PostgreSQL 16
- Docker Compose
- React 19 + Vite 7
- Tailwind CSS 3.4
- shadcn/ui-compatible component setup (`components.json` + reusable primitives)
- Recharts
- Axios

## Architecture

Hexagonal / Ports and Adapters:

`domain` → pure business concepts and ports  
`application` → use-case services  
`adapter/in` → REST + CSV input  
`adapter/out` → PostgreSQL/JPA persistence

The transaction data is immutable at the domain/API level. Category assignment is separate metadata, so assigning a category does not rewrite the imported concept/date/amount/available-balance.

## Implemented MVP

- CSV import with review/confirm/cancel
- CSV lifecycle: CSV → Importing → Examining → Added / Ignored
- duplicate detection using the currently documented `available balance` criterion
- local PostgreSQL persistence
- precise monetary values with `BigDecimal` / PostgreSQL `NUMERIC(19,2)`
- latest 50 movements
- category assignment metadata + category filtering
- latest 10 supported by the movements endpoint
- EXPENSES required category
- manual category creation
- manual transfers between categories
- negative EXPENSES visibility/correction workflow
- TOTAL MONEY changes only during confirmed CSV import
- accumulated expenses by category
- budget persistence schema ready for extension
- multiple accounts with no application-level count limit
- responsive dashboard and charts

## Important source ambiguity preserved

The source notes contain a conflict: one business-rule line says a transaction cannot be part of a category, while the requirements/acceptance criteria explicitly require filtering and accumulated expenses by category. The implementation keeps imported transaction fields immutable and models category assignment as separate metadata to support those documented requirements without changing transaction data.

The duplicate rule is implemented exactly as the QA pack currently documents: matching `available_balance`. The QA pack also calls the exact full duplicate key an open point.

## Run

```bash
docker compose up --build
```

Open:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432

Default account: `Myself`.

## Local development

Backend:
```bash
cd backend
mvn spring-boot:run
```

Frontend:
```bash
cd frontend
npm install
npm run dev
```

Set `VITE_API_URL=http://localhost:8080/api` if needed.

## API

- `GET /api/accounts`
- `POST /api/accounts`
- `GET /api/accounts/{id}`
- `GET /api/accounts/{id}/categories`
- `POST /api/accounts/{id}/categories`
- `POST /api/accounts/{id}/categories/transfer`
- `GET /api/accounts/{id}/movements?limit=50&categoryId=...`
- `POST /api/accounts/{id}/imports`
- `POST /api/accounts/{id}/imports/confirm`
- `GET /api/accounts/{id}/reports/expenses`

## Out of scope

Direct bank connection, many bank entities, investments, cryptocurrencies, fiscal management and shared accounts remain outside the initial scope.

Future ideas such as category automation, period reports, expense evolution, recurring-expense detection, goals and mobile are not silently treated as MVP requirements.
