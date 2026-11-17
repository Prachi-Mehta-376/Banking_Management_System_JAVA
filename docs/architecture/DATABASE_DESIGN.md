# Conceptual Database Design (`DATABASE_DESIGN.md`)

> **Status:** `CONCEPTUAL PROPOSAL — NO SQL SCHEMA IMPLEMENTED`
> **Primary Owner:** **P** (Database, JDBC & Reliability), coordinated with **S** on domain entities.
> **Related Decisions:** `ADR-003` (Money), `ADR-004` (Transactions), `ADR-007` (Immutable Ledger).

---

## 1. Design Philosophy

Persistence uses **SQLite** accessed exclusively via **Plain JDBC** (`java.sql.*`) and the **DAO pattern**. To preserve academic clarity and financial integrity:
- All relationships use explicit foreign keys with SQLite `PRAGMA foreign_keys = ON`.
- Financial amounts never use `REAL` or floating-point columns (`ADR-003`).
- Historical ledger/transaction rows and audit logs are append-only (`ADR-007`).

---

## 2. Conceptual Entity Relationship Overview (Proposed — Not Implemented)

```text
+------------+       1..1 (optional)      +-------------+
|   users    |--------------------------->|  customers  |
+------------+                            +-------------+
| user_id PK |                            | customer_id |
| username   |                            | full_name   |
| pass_hash  |                            | contact...  |
| role       |                            | status      |
| active     |                            +------+------+
+-----+------+                                   | 1
      |                                          |
      | acted_by / requested_by / approved_by    | *
      v                                   +------+------+
+------------+                            |  accounts   |
| audit_logs |                            +------+------+
+------------+                                   | 1
                                                 |
            +------------------------------------+------------------------------------+
            | * (source/target account)                                               | *
            v                                                                         v
+------------------------+                                                +--------------------+
|      transactions      |                                                |  freeze_requests   |
+------------------------+                                                +--------------------+
```

---

## 3. Conceptual Entities & Proposed Attributes

> Note: Exact column names, SQL DDL, indexes, and constraints will be authored by **P** in `src/main/resources/db/` only after M1 contract freeze.

### 3.1 `users` (Authentication & Role Principal)
- **Purpose:** Stores login identities across all three roles (`CUSTOMER`, `STAFF`, `ADMIN`).
- **Conceptual Attributes:** `user_id` (PK), `username` (unique), `password_hash`, `password_salt` (or combined encoded hash), `role`, `account_enabled`, `created_at`, `updated_at`.
- **Constraints:** `username` uniqueness; `role` restricted to allowed role values.

### 3.2 `customers` (Synthetic Customer Profile)
- **Purpose:** Stores synthetic customer profile metadata linked to a `CUSTOMER` user login.
- **Conceptual Attributes:** `customer_id` (PK), `user_id` (FK -> `users`, unique), `customer_code` (unique synthetic identifier), `full_name`, `email`, `phone`, `synthetic_kyc_ref`, `created_by_staff_id` (FK -> `users`), `created_at`.
- **Simulation Rule:** All profile details must be strictly synthetic.

### 3.3 `accounts` (Simulated Bank Accounts)
- **Purpose:** Represents simulated customer accounts holding balances and lifecycle status.
- **Conceptual Attributes:** `account_id` (PK), `account_number` (unique), `customer_id` (FK -> `customers`), `account_type` (default savings; expandable in P2), `balance_paise` (proposed `INTEGER` minor units per `ADR-003`), `status` (`PENDING`, `ACTIVE`, `FROZEN`, `CLOSED` — exact lifecycle subject to `RISK-05`), `created_at`, `updated_at`.
- **Constraints:** `CHECK (balance_paise >= 0)` to enforce non-negative simulated balances at the database boundary.

### 3.4 `transactions` (Immutable Financial Ledger)
- **Purpose:** Append-only record of every simulated cash deposit, withdrawal, and internal transfer.
- **Conceptual Attributes:** `transaction_id` (PK), `transaction_ref` (unique reference), `transaction_type` (`CASH_DEPOSIT`, `CASH_WITHDRAWAL`, `INTERNAL_TRANSFER`), `source_account_id` (nullable FK -> `accounts` for cash deposit), `destination_account_id` (nullable FK -> `accounts` for cash withdrawal), `amount_paise` (`INTEGER`, `CHECK (amount_paise > 0)`), `initiated_by_user_id` (FK -> `users`), `description`, `idempotency_key` (optional P2), `created_at`.
- **Integrity Rules:** `source_account_id != destination_account_id` for transfers; rows are never updated or deleted (`ADR-007`).
- **Alternative Under M1 Review:** Double-entry `ledger_entries` sub-table (one debit row + one credit row per transfer) vs. single normalized `transactions` row + balance snapshots.

### 3.5 `freeze_requests` (P1 — Maker-Checker Governance)
- **Purpose:** Tracks staff-initiated requests to freeze or unfreeze a customer account pending Administrator decision.
- **Conceptual Attributes:** `request_id` (PK), `account_id` (FK -> `accounts`), `request_type` (`FREEZE`, `UNFREEZE`), `reason`, `requested_by_user_id` (FK -> `users`, Staff), `status` (`PENDING`, `APPROVED`, `REJECTED`), `reviewed_by_user_id` (nullable FK -> `users`, Admin), `review_notes`, `created_at`, `reviewed_at`.
- **Constraint Idea:** `requested_by_user_id != reviewed_by_user_id` to prevent self-approval.

### 3.6 `audit_logs` (P1 — Operational Audit Trail)
- **Purpose:** Records security-sensitive and administrative actions for traceability.
- **Conceptual Attributes:** `audit_id` (PK), `actor_user_id` (nullable FK -> `users`), `actor_role`, `action_type`, `target_entity`, `target_id`, `outcome` (`SUCCESS`, `DENIED`, `FAILED`), `details`, `created_at`.
- **Open Question:** See `RISK-06` regarding same-transaction vs. independent audit persistence for failed operations.

---

## 4. SQLite Reliability & Pragma Proposals

- **`PRAGMA foreign_keys = ON;`** — Must be executed on every JDBC connection because SQLite defaults to foreign keys off per connection.
- **`PRAGMA journal_mode = WAL;`** — Proposed to improve read concurrency while preserving transactional safety (subject to `P` testing).
- **`PRAGMA busy_timeout = 5000;`** — Proposed to mitigate transient lock contention (`RISK-03`).

---

## 5. Monetary Column Storage Options (Pending `ADR-003` Sign-Off)

| Option | SQLite Column Type | Pros | Cons |
| :--- | :--- | :--- | :--- |
| **Option A (Recommended)** | `INTEGER` (`balance_paise`) | Exact integer arithmetic, native `SUM()` reconciliation, reliable `CHECK >= 0` | Requires `* 100` / `/ 100` conversion in DAO mappers |
| **Option B** | `TEXT` (`"1250.50"`) | Human-readable in raw DB browser | Cannot reliably use SQL numeric `CHECK` or exact `SUM()` without casting pitfalls |
