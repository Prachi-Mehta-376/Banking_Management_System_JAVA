# Relational Database Design (`DATABASE_DESIGN.md`)

> **Version:** M1 v2.0 Specification (`PROPOSED — NO SQL SCHEMA IMPLEMENTED`)
> **Primary Owner:** **P** (Database, JDBC & Reliability), coordinated with **S** on domain entities and invariants.
> **Related Decisions:** `ADR-003` (Integer Paise), `ADR-004` (`TransactionExecutor`), `ADR-007` (Balance-Plus-Journal).
> **Restriction Note:** Do **not** generate `schema.sql` during M1; DDL scripts belong to `src/main/resources/db/` in M2.

---

## 1. Design Principles & SQLite Connection Requirements

- **Plain JDBC & SQLite:** Five core P0 tables (`users`, `accounts`, `operations`, `transactions`, `audit_events`) plus one optional P1 table (`account_action_requests`).
- **Mandatory Per-Connection Foreign Key Enforcement:** SQLite disables foreign keys per connection by default. Every connection opened by `db/` **must** execute `PRAGMA foreign_keys = ON;` before any query or DML runs.
- **Deletion Protection:** Financial history and actor audit trails are preserved by setting `ON DELETE RESTRICT` on foreign keys referencing `users`, `accounts`, and `operations`. Accounts and staff identities are deactivated (`is_active = 0` or `FROZEN`) rather than physically deleted.
- **Timestamp Representation:** All `created_at` (and P1 `reviewed_at`) columns use **ISO-8601 UTC text strings** (`YYYY-MM-DDTHH:MM:SS.sssZ`, generated via `java.time.Instant.now().toString()`), providing human readability, timezone consistency, and lexicographical sortability in SQLite.
- **Monetary Representation:** All currency columns (`balance_paise`, `delta_paise`, `balance_after_paise`) store signed/non-negative **64-bit integer paise** (`INTEGER` in SQLite; `long` in Java) per `ADR-003`. Zero `REAL` columns exist.

---

## 2. Conceptual Entity Relationship Diagram (M1 v2.0 — Proposed)

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
          |                         | UQ idempotency_key |     |    balance_after>=0|
          |                         |    created_at      |     |    created_at      |
          |                         +--------------------+     +--------------------+
          |
          | actor_user_id (1..*)    +--------------------+
          +------------------------>|    audit_events    |
                                    +--------------------+
```

---

## 3. Core Relational Table Specifications (P0)

### 3.1 `users`
Stores authentication credentials, role assignment, and identity profile for `CUSTOMER`, `STAFF`, and `ADMIN` principals (merging customer profile data into `users` per M1 simplification).

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate user ID |
| `full_name` | `TEXT` | `NOT NULL`, `CHECK (length(trim(full_name)) > 0)` | Display name (synthetic for customers) |
| `email` | `TEXT` | `NOT NULL UNIQUE`, normalized lowercase before insert | Login identifier & search attribute |
| `password_hash` | `TEXT` | `NOT NULL` | Base64/Hex encoded derived key (`PBKDF2WithHmacSHA256`) |
| `password_salt` | `TEXT` | `NOT NULL` | Random cryptographic per-user salt |
| `password_algo` | `TEXT` | `NOT NULL` | Hash algorithm & iteration metadata (e.g., `PBKDF2WithHmacSHA256:65536`) |
| `role` | `TEXT` | `NOT NULL`, `CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'))` | Fixed RBAC role |
| `is_active` | `INTEGER` | `NOT NULL DEFAULT 1`, `CHECK (is_active IN (0, 1))` | Enables soft deactivation (e.g., staff deactivation) |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC creation timestamp |

- **Indexes:** Implicit unique index on `email`; secondary index `idx_users_role_active(role, is_active)` and `idx_users_full_name(full_name)` for staff search.

### 3.2 `accounts`
Stores simulated customer bank accounts. One customer holds **at most one account** in v1 (`UNIQUE(user_id)`). Staff and Admin users do not hold accounts.

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate account ID |
| `user_id` | `INTEGER` | `NOT NULL UNIQUE`, `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT` | Owning customer user (enforces 1 account/customer in v1) |
| `account_number` | `TEXT` | `NOT NULL UNIQUE`, `CHECK (length(trim(account_number)) > 0)` | External simulated account identifier |
| `balance_paise` | `INTEGER` | `NOT NULL DEFAULT 0`, `CHECK (balance_paise >= 0)` | Current balance in minor units (starts at `0`) |
| `status` | `TEXT` | `NOT NULL DEFAULT 'ACTIVE'`, `CHECK (status IN ('ACTIVE', 'FROZEN'))` | v1 account operational status |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC creation timestamp |

- **Indexes:** Implicit unique indexes on `user_id` and `account_number`.

### 3.3 `operations`
Represents the parent business event for every committed financial action (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER`).

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate operation ID |
| `reference_id` | `TEXT` | `NOT NULL UNIQUE` | Unique human/receipt reference (e.g., UUID or timestamp-prefixed ref) |
| `operation_type` | `TEXT` | `NOT NULL`, `CHECK (operation_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER'))` | Categorizes inflow, outflow, or internal transfer |
| `performed_by` | `INTEGER` | `NOT NULL`, `FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE RESTRICT` | Authenticated Staff (deposit/withdrawal) or Customer (transfer) |
| `idempotency_key` | `TEXT` | `UNIQUE` (nullable in P0; populated if P1 idempotency is enabled) | Distinguishes client retry keys from server `reference_id` |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC execution timestamp |

- **Indexes:** Implicit unique indexes on `reference_id` and `idempotency_key`; index `idx_operations_performed_by(performed_by, created_at)`.

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

- **Composite Uniqueness:** `UNIQUE (operation_id, account_id)` guarantees at most one journal entry per account per operation (preventing duplicate debit/credit on the same account in v1).
- **Indexes:** Composite index `idx_transactions_account_created(account_id, created_at DESC, id DESC)` for fast customer history queries and reconciliation sums.

### 3.5 `audit_events`
Append-only security and governance log linking sensitive actions to the acting user.

| Column | SQLite Type | Constraints | Purpose |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Surrogate audit ID |
| `actor_user_id` | `INTEGER` | `NOT NULL`, `FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE RESTRICT` | Authenticated user performing the action |
| `action` | `TEXT` | `NOT NULL`, `CHECK (length(trim(action)) > 0)` | Action code (e.g., `INITIAL_ADMIN_CREATED`, `STAFF_CREATED`, `STAFF_DEACTIVATED`, `ACCOUNT_FROZEN`) |
| `target_type` | `TEXT` | `NOT NULL` | Affected entity type (`USER`, `ACCOUNT`, `ACTION_REQUEST`) |
| `target_id` | `TEXT` | `NOT NULL` | String representation of affected entity identifier |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC timestamp |

- **Indexes:** `idx_audit_events_created(created_at DESC, id DESC)` and `idx_audit_events_actor(actor_user_id, created_at DESC)`.

---

## 4. Optional Future Table (P1 — Not a P0 Dependency)

### `account_action_requests` (Conceptual P1 Maker-Checker Support)
Supports staff-initiated freeze/unfreeze requests reviewed by an administrator without blocking any P0 schema or feature:
- Columns: `id` (`INTEGER PK`), `account_id` (`FK -> accounts`), `action_type` (`CHECK (action_type IN ('FREEZE', 'UNFREEZE'))`), `reason` (`TEXT NOT NULL`), `requested_by` (`FK -> users`), `status` (`CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))`), `reviewed_by` (`FK -> users NULL`), `created_at`, `reviewed_at` (`NULL`).
- Constraints: `CHECK (reviewed_by IS NULL OR reviewed_by != requested_by)` prevents self-approval; partial unique index idea (`UNIQUE(account_id) WHERE status = 'PENDING'`) prevents duplicate concurrent pending requests for one account.

---

## 5. Database-Level Constraints vs. Application-Level Invariants

| Concern | Enforced by SQLite Constraints | Enforced by Service Layer (`S`) |
| :--- | :--- | :--- |
| Non-negative balances (`INV-01`) | `CHECK (balance_paise >= 0)` & `CHECK (balance_after_paise >= 0)` | Pre-check + conditional SQL update `WHERE balance_paise >= ?` |
| Zero/invalid deltas (`INV-02`) | `CHECK (delta_paise != 0)` | `Money` rejects `<= 0`, `> 2` decimals, and `long` overflow |
| 1 account per user in v1 | `UNIQUE(user_id)` on `accounts` | `AuthService.registerCustomer` creates user + 1 account; only `CUSTOMER` owns accounts |
| Only `CUSTOMER` holds accounts | *Not enforceable purely via SQLite CHECK across tables without triggers* | Service creates `accounts` rows **exclusively** inside customer registration |
| Transfer conservation (`INV-04`) | `UNIQUE(operation_id, account_id)` prevents same-account double posting | Service writes exactly 2 `transactions` rows summing to `0` (`-delta`, `+delta`) in one transaction |
| Frozen account restriction (`INV-07`) | `CHECK (status IN ('ACTIVE', 'FROZEN'))` | Service verifies `status == ACTIVE` + conditional `UPDATE ... WHERE status = 'ACTIVE'` |
| Append-only history (`INV-09`) | `ON DELETE RESTRICT` prevents cascading delete | `OperationDao`, `TransactionDao`, `AuditEventDao` expose `insert` + `find` only (no `update`/`delete`) |

---

## 6. Correctness Questions — Resolution & Open Status

| Question | M1 Proposed Resolution or Flagged Risk | Status |
| :--- | :--- | :---: |
| **1. Atomic customer user + account creation** | `AuthService.registerCustomer` wraps `UserDao.insert` and `AccountDao.insert` inside a single `TransactionExecutor` unit of work (`balance_paise = 0`, `status = 'ACTIVE'`). Failure in either step rolls back both. | `PROPOSED (S+P)` |
| **2. Unique account number generation** | Generate a structured unique account number (e.g., deterministic prefix + formatted sequence derived from DB sequence/ID or collision-checked random digits) backed by `UNIQUE(account_number)`. Exact format (e.g., `TIET` + zero-padded digits vs. 12-digit numeric) to be confirmed in M1 review. | `OPEN FOR M1 REVIEW` |
| **3. Append-only financial entries** | Application DAOs omit update/delete methods on `operations`, `transactions`, and `audit_events`; `ON DELETE RESTRICT` guards references. Direct SQLite DB file tampering via external CLI tools remains an inherent desktop simulation limitation (`RISK-03`). | `PROPOSED (With Limitation)` |
| **4. Safe balance updates without lost updates** | Perform updates via SQL arithmetic (`balance_paise = balance_paise + ?`) inside an immediate/exclusive SQLite write transaction with `WHERE id = ? AND status = 'ACTIVE'` (and `AND balance_paise >= ?` for debits) and verify `rowsAffected == 1`. Avoid blind read-modify-write overwrites. | `PROPOSED (S+P)` |
| **5. Shared operation references on transfers** | Transfers insert 1 `operations` row (`reference_id`, `operation_type = 'TRANSFER'`) and 2 `transactions` rows referencing that same `operation_id` (`-delta` for source, `+delta` for destination). | `PROPOSED` |
| **6. Deposit/withdrawal ledger honesty** | Cash deposit (`+delta`) and cash withdrawal (`-delta`) represent simplified branch inflow/outflow events (`1` operation + `1` `transactions` row). Documentation explicitly states this is **not** a bank-wide double-entry general ledger with vault/cash contra accounts. | `RESOLVED IN SPEC` |
| **7. Database constraints protecting balances** | `accounts.balance_paise >= 0`, `transactions.balance_after_paise >= 0`, `transactions.delta_paise != 0`, and conditional SQL `WHERE balance_paise >= ?` block negative balances even if a service bug misses a pre-check. | `PROPOSED` |
| **8. `reference_id` vs. client `idempotency_key`** | Kept **distinct**: `operations.reference_id` is always generated server-side as the receipt identifier; `operations.idempotency_key` is a separate optional client-supplied retry token (`UNIQUE NULL`) reserved for P1 idempotent request handling (`F-P1-05`). | `RESOLVED IN SPEC` |
| **9. Restricting user/account deletion** | All foreign keys pointing to `users`, `accounts`, and `operations` specify `ON DELETE RESTRICT`; staff accounts deactivate via `is_active = 0` and accounts freeze via `status = 'FROZEN'`. Account closure is explicitly out of P0 scope. | `PROPOSED` |
