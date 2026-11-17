# Relational Database Design (`DATABASE_DESIGN.md`)

> **Version:** M1 v2.0 Finalized Contract (`RECOMMENDED — AWAITING PR #1 APPROVAL`)
> **Primary Owner:** **P** (Database, JDBC & Reliability), coordinated with **S** on domain entities and invariants.
> **Related Decisions:** `ADR-003` (Integer Paise), `ADR-004` (`TransactionExecutor`), `ADR-007` (Balance-Plus-Journal), `ADR-008` (Bootstrap & Lifecycle).
> **Restriction Note:** Do **not** generate `schema.sql` during M1; DDL scripts belong to `src/main/resources/db/` in M2.

---

## 1. Design Principles & SQLite Connection Requirements

- **Five Core P0 Tables:** `users`, `accounts`, `operations`, `transactions`, `audit_events` (plus optional P1 `account_action_requests` and P1 `operations.idempotency_key` extension).
- **Mandatory Per-Connection Foreign Key Enforcement:** SQLite disables foreign keys per connection by default. Every connection opened by `db/` **must** execute `PRAGMA foreign_keys = ON;` before any query or DML runs.
- **Deletion Protection:** Financial history and actor audit trails are preserved via `ON DELETE RESTRICT` on foreign keys referencing `users`, `accounts`, and `operations`. Accounts and staff identities are deactivated (`is_active = 0` or `FROZEN`) rather than physically deleted.
- **Timestamp Representation:** All `created_at` (and P1 `reviewed_at`) columns use **ISO-8601 UTC text strings** (`YYYY-MM-DDTHH:MM:SS.sssZ`, generated via `java.time.Instant.now().toString()`), providing readability, timezone consistency, and lexicographical sortability.
- **Monetary Representation:** All currency columns (`balance_paise`, `delta_paise`, `balance_after_paise`) store signed/non-negative **64-bit integer paise** (`INTEGER` in SQLite; `long` in Java) per `ADR-003`. Zero `REAL` columns exist.

---

## 2. Conceptual Entity Relationship Diagram (M1 v2.0)

```text
+--------------------+         0..1 (CUSTOMER only)         +-----------------------+
|       users        |------------------------------------->|       accounts        |
+--------------------+                                      +-----------------------+
| PK id              |                                      | PK id                 |
| UQ email           |                                      | FK/UQ user_id         |
|    full_name       |                                      | UQ account_number     |
|    password_hash   |                                      |    balance_paise >= 0 |
|    password_salt   |                                      |    status             |
|    password_algo   |                                      |    created_at         |
|    role            |                                      +-----------+-----------+
|    is_active       |                                                  |
|    created_at      |                                                  | 1..*
+---------+----------+                                                  |
          |                                                             v
          | performed_by (1..*)     +--------------------+     +--------------------+
          +------------------------>|     operations     |---->|    transactions    |
          |                         +--------------------+ 1:* +--------------------+
          |                         | PK id              |     | PK id              |
          |                         | UQ reference_id    |     | FK operation_id    |
          |                         |    operation_type  |     | FK account_id      |
          |                         | FK performed_by    |     |    delta_paise !=0 |
          |                         |    created_at      |     |    balance_after>=0|
          |                         +--------------------+     |    created_at      |
          |                                                    +--------------------+
          | actor_user_id (1..*)    +--------------------+
          +------------------------>|    audit_events    |
                                    +--------------------+
```

---

## 3. Core Relational Table Specifications (P0)

### 3.1 `users`
Stores authentication credentials, role assignment, and identity profile for `CUSTOMER`, `STAFF`, and `ADMIN` principals.

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate user ID |
| `full_name` | `TEXT` | `NOT NULL`, `CHECK (length(trim(full_name)) > 0)` | Display name (fictitious for simulated customers) |
| `email` | `TEXT` | `NOT NULL UNIQUE`, normalized lowercase before insert | Login identifier & search attribute |
| `password_hash` | `TEXT` | `NOT NULL` | Encoded derived key (`PBKDF2WithHmacSHA256`) |
| `password_salt` | `TEXT` | `NOT NULL` | Random cryptographic per-user salt |
| `password_algo` | `TEXT` | `NOT NULL` | Hash algorithm & iteration metadata (e.g., `PBKDF2WithHmacSHA256:65536`) |
| `role` | `TEXT` | `NOT NULL`, `CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'))` | Fixed RBAC role |
| `is_active` | `INTEGER` | `NOT NULL DEFAULT 1`, `CHECK (is_active IN (0, 1))` | Soft deactivation (used for staff deactivation) |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC creation timestamp |

- **Indexes:** Implicit unique index on `email`; `idx_users_role_active(role, is_active)` and `idx_users_full_name(full_name)` for search.

### 3.2 `accounts`
Stores simulated customer bank accounts. One customer holds **at most one account** in v1 (`UNIQUE(user_id)`). Staff and Admin users do not hold accounts.

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate account ID (source for `AC000001` formatting) |
| `user_id` | `INTEGER` | `NOT NULL UNIQUE`, `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT` | Owning customer user (enforces `<= 1` account/customer in v1) |
| `account_number` | `TEXT` | `NOT NULL UNIQUE`, `CHECK (length(trim(account_number)) > 0)` | Stable public routing identifier (`AC000001` format) |
| `balance_paise` | `INTEGER` | `NOT NULL DEFAULT 0`, `CHECK (balance_paise >= 0)` | Current balance in minor units (starts at `0`) |
| `status` | `TEXT` | `NOT NULL DEFAULT 'ACTIVE'`, `CHECK (status IN ('ACTIVE', 'FROZEN'))` | v1 account operational status |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC creation timestamp |

- **Indexes:** Implicit unique indexes on `user_id` and `account_number`.

### 3.3 `operations`
Parent business event for every committed financial operation (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER`).

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate operation ID |
| `reference_id` | `TEXT` | `NOT NULL UNIQUE` | Unique server-generated operation/receipt reference |
| `operation_type` | `TEXT` | `NOT NULL`, `CHECK (operation_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER'))` | Categorizes inflow, outflow, or internal transfer |
| `performed_by` | `INTEGER` | `NOT NULL`, `FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE RESTRICT` | Authenticated Staff (deposit/withdrawal) or Customer (transfer) |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC execution timestamp |

- **Indexes:** Implicit unique index on `reference_id`; `idx_operations_performed_by(performed_by, created_at)`.

### 3.4 `transactions`
Append-only signed account-level journal entries linked to a parent `operations` row. v1 writes **one entry per affected account per operation**.

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate journal entry ID |
| `operation_id` | `INTEGER` | `NOT NULL`, `FOREIGN KEY (operation_id) REFERENCES operations(id) ON DELETE RESTRICT` | Parent business operation |
| `account_id` | `INTEGER` | `NOT NULL`, `FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT` | Affected customer account |
| `delta_paise` | `INTEGER` | `NOT NULL`, `CHECK (delta_paise != 0)` | Signed amount change (`+` credit, `-` debit) |
| `balance_after_paise` | `INTEGER` | `NOT NULL`, `CHECK (balance_after_paise >= 0)` | Resulting account balance immediately after this operation |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC timestamp matching operation |

- **Composite Uniqueness:** `UNIQUE (operation_id, account_id)` guarantees at most one entry per account per operation in v1.
- **Indexes:** `idx_transactions_account_created(account_id, created_at DESC, id DESC)` supporting history queries and P1 `SUM(delta_paise)` reconciliation.

### 3.5 `audit_events`
Append-only security and governance log linking sensitive actions to the acting user (`INV-10`).

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate audit ID |
| `actor_user_id` | `INTEGER` | `NOT NULL`, `FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE RESTRICT` | Authenticated user performing the controlled change |
| `action` | `TEXT` | `NOT NULL`, `CHECK (length(trim(action)) > 0)` | Action code (`INITIAL_ADMIN_CREATED`, `STAFF_CREATED`, `STAFF_DEACTIVATED`, `STAFF_ACTIVATED`, etc.) |
| `target_type` | `TEXT` | `NOT NULL` | Affected entity type (`USER`, `ACCOUNT`, `ACTION_REQUEST`) |
| `target_id` | `TEXT` | `NOT NULL` | String identifier of affected entity |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC timestamp |

- **Indexes:** `idx_audit_events_created(created_at DESC, id DESC)` and `idx_audit_events_actor(actor_user_id, created_at DESC)`.

---

## 4. Optional Future P1 Schema Extensions (Not P0 Dependencies)

1. **`account_action_requests` Table (P1 Maker-Checker):**
   - Columns: `id` (`INTEGER PK`), `account_id` (`FK -> accounts`), `action_type` (`CHECK (action_type IN ('FREEZE', 'UNFREEZE'))`), `reason` (`TEXT NOT NULL`), `requested_by` (`FK -> users`), `status` (`CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))`), `reviewed_by` (`FK -> users NULL`), `created_at`, `reviewed_at` (`NULL`).
   - Constraints: `CHECK (reviewed_by IS NULL OR reviewed_by != requested_by)` prevents self-approval.
2. **`operations.idempotency_key` Column (P1 Idempotency):**
   - Nullable `TEXT UNIQUE` column on `operations` added when P1 persistent idempotency (`F-P1-05`) is implemented, completely separate from `operations.reference_id`. P0 APIs and schema omit client-supplied idempotency keys (`Decision B`).

---

## 5. Database-Level Constraints vs. Application-Level Invariants

| Concern | Enforced by SQLite Constraints | Enforced by Service Layer (`S`) |
| :--- | :--- | :--- |
| Non-negative balances (`INV-01`) | `CHECK (balance_paise >= 0)` & `CHECK (balance_after_paise >= 0)` | Pre-check + conditional SQL update `WHERE balance_paise >= ?` |
| Zero/invalid deltas (`INV-02`) | `CHECK (delta_paise != 0)` | `Money` rejects `<= 0`, `> 2` decimals, and `long` overflow |
| 1 account per user in v1 | `UNIQUE(user_id)` on `accounts` | `AuthService.registerCustomer` creates user + 1 account atomically |
| Only `CUSTOMER` holds accounts | *Not enforced via cross-table SQLite CHECK* | Service creates `accounts` rows **exclusively** inside customer registration |
| Transfer conservation (`INV-04`) | `UNIQUE(operation_id, account_id)` blocks duplicate account posting | Service posts exactly 2 `transactions` rows summing to `0` (`-delta`, `+delta`) in one transaction |
| Frozen account restriction (`INV-07`) | `CHECK (status IN ('ACTIVE', 'FROZEN'))` | Service verifies `status == ACTIVE` + conditional `UPDATE ... WHERE status = 'ACTIVE'` |
| Append-only history (`INV-09`) | `ON DELETE RESTRICT` blocks deletion | `OperationDao`, `TransactionDao`, `AuditEventDao` expose insert/read only |

---

## 6. M1 Correctness Decisions (Resolved in Spec — Awaiting Approval)

| Question | Finalized M1 Contract Resolution | Decision Status |
| :--- | :--- | :---: |
| **1. Atomic customer user + account creation** | `AuthService.registerCustomer` executes `UserDao.insert` and `AccountDao.createAccount` inside one `TransactionExecutor` block (`balance_paise = 0`, `status = 'ACTIVE'`). Failure rolls back both. | `RESOLVED (Decision A)` |
| **2. Unique account number generation (`Decision A`)** | Generated deterministically from the assigned `accounts.id`: `"AC" + String.format("%06d", id)` (e.g., `AC000001`). Inside `AccountDao.createAccount` (within the active transaction): insert row with temporary unique placeholder, read generated `id` via `getGeneratedKeys()`, update `account_number` to `AC<6+ digits>` in the same transaction. Backed by `UNIQUE(account_number)`, `AUTOINCREMENT`, never reused, and treated as a public routing identifier rather than an authentication secret. | `RESOLVED (Decision A)` |
| **3. Append-only financial entries** | Application DAOs omit update/delete methods on `operations`, `transactions`, and `audit_events`; `ON DELETE RESTRICT` guards FKs. External SQLite CLI edits remain an acknowledged desktop simulation limitation (`RISK-03`). | `RESOLVED (With Limitation)` |
| **4. Safe balance updates without lost updates** | Conditional SQL arithmetic (`SET balance_paise = balance_paise + ? WHERE id = ? AND status = 'ACTIVE'` for credits; `SET balance_paise = balance_paise - ? WHERE id = ? AND status = 'ACTIVE' AND balance_paise >= ?` for debits) inside an immediate SQLite write transaction with `rowsAffected == 1` verification. | `RESOLVED (S+P)` |
| **5. Shared operation references on transfers** | Transfers insert 1 `operations` row (`reference_id`, `operation_type = 'TRANSFER'`) and 2 `transactions` rows referencing that `operation_id` (`-delta` source, `+delta` destination). | `RESOLVED` |
| **6. Deposit/withdrawal ledger honesty** | Cash deposit (`+delta`) and withdrawal (`-delta`) represent simulated external inflows/outflows (`1` operation + `1` journal entry), explicitly **not** a bank-wide double-entry general ledger. | `RESOLVED` |
| **7. Database constraints protecting balances** | `accounts.balance_paise >= 0`, `transactions.balance_after_paise >= 0`, `transactions.delta_paise != 0`, and conditional SQL `WHERE balance_paise >= ?` prevent negative balances. | `RESOLVED` |
| **8. Operation `reference_id` vs. idempotency key (`Decision B`)** | Distinct concepts: P0 generates a unique server-side `reference_id` for every committed operation and does **not** take a client `idempotencyKey`. Client idempotency (`idempotency_key`) is deferred to P1 (`F-P1-05`). | `RESOLVED (Decision B)` |
| **9. Restricting user/account deletion** | Foreign keys use `ON DELETE RESTRICT`; staff deactivate via `is_active = 0` and accounts freeze via `status = 'FROZEN'`. Account closure is out of P0 scope. | `RESOLVED` |
