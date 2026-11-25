CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL CHECK (length(trim(full_name)) > 0),
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    password_algo TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN')),
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_role_active ON users(role, is_active);
CREATE INDEX IF NOT EXISTS idx_users_full_name ON users(full_name);

CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    account_number TEXT NOT NULL UNIQUE CHECK (length(trim(account_number)) > 0),
    balance_paise INTEGER NOT NULL DEFAULT 0 CHECK (balance_paise >= 0),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FROZEN')),
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS operations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reference_id TEXT NOT NULL UNIQUE,
    operation_type TEXT NOT NULL CHECK (operation_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    performed_by INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_operations_performed_by ON operations(performed_by, created_at);

CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operation_id INTEGER NOT NULL REFERENCES operations(id) ON DELETE RESTRICT,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    delta_paise INTEGER NOT NULL CHECK (delta_paise != 0),
    balance_after_paise INTEGER NOT NULL CHECK (balance_after_paise >= 0),
    created_at TEXT NOT NULL,
    UNIQUE (operation_id, account_id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_account_created ON transactions(account_id, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS audit_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action TEXT NOT NULL CHECK (length(trim(action)) > 0),
    target_type TEXT NOT NULL,
    target_id TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_events_created ON audit_events(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events(actor_user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS account_action_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    action_type TEXT NOT NULL CHECK (action_type IN ('FREEZE', 'UNFREEZE')),
    reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
    requested_by INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TEXT NOT NULL,
    reviewed_at TEXT,
    CHECK (reviewed_by IS NULL OR reviewed_by != requested_by)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_one_pending_request_per_account
    ON account_action_requests(account_id)
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS idempotency_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    idempotency_key TEXT NOT NULL,
    operation_type TEXT NOT NULL CHECK (operation_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    request_signature TEXT NOT NULL,
    reference_id TEXT NOT NULL REFERENCES operations(reference_id) ON DELETE RESTRICT,
    primary_account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    counterparty_account_number TEXT,
    amount_paise INTEGER NOT NULL CHECK (amount_paise > 0),
    balance_after_paise INTEGER NOT NULL CHECK (balance_after_paise >= 0),
    created_at TEXT NOT NULL,
    UNIQUE (user_id, idempotency_key)
);
