CREATE TABLE accounts (
 id BIGSERIAL PRIMARY KEY,
 holder VARCHAR(160) NOT NULL,
 total_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
 last_modification TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE categories (
 id BIGSERIAL PRIMARY KEY,
 account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 description VARCHAR(120) NOT NULL,
 available_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
 general_expenses BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE(account_id, description)
);
CREATE TABLE movements (
 id BIGSERIAL PRIMARY KEY,
 account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 concept VARCHAR(255) NOT NULL,
 movement_date DATE NOT NULL,
 amount NUMERIC(19,2) NOT NULL,
 available_balance NUMERIC(19,2) NOT NULL,
 import_status VARCHAR(30) NOT NULL,
 category_id BIGINT NULL REFERENCES categories(id) ON DELETE SET NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_movements_account_date ON movements(account_id, movement_date DESC);
CREATE INDEX idx_movements_account_available_balance ON movements(account_id, available_balance);
CREATE INDEX idx_movements_category ON movements(category_id);
CREATE TABLE budgets (
 id BIGSERIAL PRIMARY KEY,
 category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
 period_start DATE NOT NULL,
 period_end DATE NOT NULL,
 amount NUMERIC(19,2) NOT NULL,
 UNIQUE(category_id, period_start, period_end)
);
INSERT INTO accounts(holder,total_balance) VALUES ('Myself',0);
INSERT INTO categories(account_id,description,available_balance,general_expenses)
SELECT id,'EXPENSES',0,TRUE FROM accounts WHERE holder='Myself';
