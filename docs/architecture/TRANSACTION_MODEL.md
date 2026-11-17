# Financial & Transaction Management Model (`TRANSACTION_MODEL.md`)

> **Version:** M1 v2.0 Specification (`PROPOSED — AWAITING TEAM REVIEW`)
> **Related Decisions:** `ADR-003` (Integer Paise), `ADR-004` (`TransactionExecutor`), `ADR-007` (Balance-Plus-Journal).
> **Note:** Defines design contracts and target invariants (`INV-01`–`INV-12`) without executable Java/SQL code.

---

## 1. Monetary Representation (`ADR-003`)

- **Persisted & Internal Unit:** All monetary balances and transaction deltas are stored and calculated as **64-bit signed integer paise** (`long` in Java, `INTEGER` in SQLite; `₹1.00 = 100 paise`).
- **Boundary Parsing:** User-entered amounts are parsed using `java.math.BigDecimal` (wrapped in a `Money` value class), rejecting:
  - `null`, blank, or non-numeric strings
  - Negative or zero values (`<= 0`) for financial operations (`INV-02`)
  - More than 2 decimal places (`scale() > 2`)
  - Values exceeding `long` paise range (`ArithmeticException` -> validation error)
- **Arithmetic Safety:** Balance calculations use `Math.addExact` / `Math.subtractExact` (`INV-12`), never `float` or `double`.

---

## 2. Balance-Plus-Transaction-Journal Model (`ADR-007`)

Every successful financial operation atomically creates:
1. **One** `operations` row (`reference_id`, `operation_type`, `performed_by`, `created_at`).
2. **One or more** signed `transactions` rows (`operation_id`, `account_id`, `delta_paise`, `balance_after_paise`, `created_at`).
3. **Atomic updates** to `accounts.balance_paise` for every affected account.

### Operation Examples
- **Simulated Cash Deposit ₹500 (`50000` paise):**
  - `1` row in `operations` (`operation_type = 'DEPOSIT'`, `performed_by = staffUserId`)
  - `1` row in `transactions` (`delta_paise = +50000`, `balance_after_paise = newBalance`)
  - Target account `balance_paise` incremented by `50000`
- **Simulated Cash Withdrawal ₹200 (`20000` paise):**
  - `1` row in `operations` (`operation_type = 'WITHDRAWAL'`, `performed_by = staffUserId`)
  - `1` row in `transactions` (`delta_paise = -20000`, `balance_after_paise = newBalance`)
  - Target account `balance_paise` decremented by `20000`
- **Internal Transfer ₹1,000 (`100000` paise) from Account A to Account B:**
  - `1` row in `operations` (`operation_type = 'TRANSFER'`, `performed_by = customerAUserId`)
  - `2` rows in `transactions` sharing the same `operation_id`:
    - Account A entry: `delta_paise = -100000`
    - Account B entry: `delta_paise = +100000`
  - Account A decremented by `100000` and Account B incremented by `100000` in the same transaction
  - Transfer conservation invariant (`INV-04`): `sum(delta_paise for operation) = 0`

> **Scope Honesty Note:** This is a **balance-plus-transaction-journal** model, **not** a full bank-wide double-entry general ledger. Simulated cash deposits and withdrawals act as simplified external inflows and outflows without modeling vault cash or reserve contra accounts.

---

## 3. Reconciliation Model (Planned P1)

Because all v1 customer accounts initialize with `balance_paise = 0`, expected balance derives purely from committed journal entries:

```text
expected_balance_paise = SUM(transactions.delta_paise WHERE account_id = ?)
```

`ReconciliationService` (P1) compares `expected_balance_paise` against `accounts.balance_paise`:
- If they match: report account as reconciled.
- If they differ: **report the mismatch** with stored balance, expected balance, and delta discrepancy; **never** silently overwrite `accounts.balance_paise` (`INV-11`), preserving full data for investigation.
- **Limitation:** Because SQLite is a local desktop file, external manual inspection or modification by an OS user with filesystem access cannot be cryptographically prevented (`RISK-03`).

---

## 4. Transaction Management Contract (`ADR-004`)

### 4.1 Core Abstractions & Ownership Split
- **Abstractions (`com.tiet.banking.db`):**
  - `TransactionExecutor` — Executes a unit of work atomically and manages connection/transaction boundaries.
  - `TransactionWork<T>` — Functional callback receiving a `BankingUnitOfWork` and returning result `T`.
  - `BankingUnitOfWork` — Scopes DAO access (or binds the single active JDBC `Connection`) so every DAO called inside the work callback runs on the **exact same `Connection`**.

| Responsibility | Owner | Scope |
| :--- | :---: | :--- |
| Connection creation, `PRAGMA foreign_keys = ON`, `BEGIN IMMEDIATE` / `setAutoCommit(false)`, `commit()`, `rollback()`, `close()`, `SQLITE_BUSY` handling | **P** | `db/` & `dao/jdbc/` |
| Authentication, RBAC checks, `Money` validation, business rules, orchestration, journal entry construction, custom exceptions | **S** | `service/` & `security/` |

### 4.2 Why All Participating DAOs Must Share One Connection
In SQLite + JDBC, a transaction is strictly scoped to a single `java.sql.Connection`. If `AccountDao` and `TransactionDao` acquire separate connections, SQLite treats them as independent sessions—meaning:
1. A failure in `TransactionDao` could leave `AccountDao`'s debit committed (`INV-03` violated).
2. A second connection attempting a write while the first connection holds an uncommitted write transaction immediately hits `SQLITE_BUSY` deadlock/lock errors.

### 4.3 Mandatory 9-Step Financial Execution Sequence
1. **Authenticate & Authorize:** Service verifies active `UserSession`, required `Role`, and (for customer operations) ownership (`INV-05`, `INV-06`).
2. **Validate Input:** Parse and validate `Money` (`> 0`, `<= 2` decimals, overflow-safe) and non-empty distinct account identifiers (`INV-02`).
3. **Enter Database Transaction:** Invoke `TransactionExecutor.execute(...)`, which acquires one `Connection` with `PRAGMA foreign_keys = ON` and disables auto-commit (using `BEGIN IMMEDIATE` or equivalent SQLite write-lock acquisition to prevent write-upgrade deadlocks).
4. **Re-Read Account State Inside Transaction:** Load target account(s) on the active transaction connection (for transfers, lock/read accounts in consistent ID order).
5. **Check Status & Funds:** Verify all participating accounts have `status == ACTIVE` (`INV-07`) and, for withdrawals/transfers, `balance_paise >= amount_paise` (`INV-01`).
6. **Safely Update Balances:** Execute atomic conditional `UPDATE`s (e.g., `WHERE id = ? AND status = 'ACTIVE' AND balance_paise >= ?` for debits; `WHERE id = ? AND status = 'ACTIVE'` for credits) with overflow checks (`INV-12`) and verify `rowsAffected == 1`.
7. **Write Operation & Journal Records:** Insert `1` row into `operations` and `1` (deposit/withdrawal) or `2` (transfer) rows into `transactions` with accurate `balance_after_paise` (`INV-08`).
8. **Commit:** Call `connection.commit()` **only** after every required write succeeds and `rowsAffected == 1` checks pass.
9. **Rollback on Failure:** Catch any runtime/business/SQL exception before commit, execute `connection.rollback()`, preserve clean connection cleanup, and map persistence failures safely without exposing raw SQL internals (`INV-03`).

---

## 5. Reliability Edge Cases & Failure Semantics

- **Affected-Row Verification:** Every conditional balance `UPDATE` must assert `executeUpdate() == 1`. If `0` rows updated (e.g., concurrent balance change or frozen status), abort and roll back immediately.
- **SQLite Write Locks & `SQLITE_BUSY`:** SQLite permits one active writer at a time. `db/` sets a reasonable `busy_timeout` and starts write transactions cleanly (`BEGIN IMMEDIATE` where supported) so concurrent withdrawals serialize safely (`TC-12`) rather than corrupting balances.
- **Failure Injection for Verification:** `TransactionExecutor` or service/DAO integration tests (`TC-10`, `TC-11`) can inject a controlled failure (e.g., failing callback step after debit before commit) via test stubs/hooks without embedding production backdoors.
- **Pre-Commit vs. Uncertain Commit Outcomes & No Unsafe Retries:**
  - If an error occurs **before** `commit()` is invoked, rollback guarantees zero committed change; the UI can safely report failure and allow user resubmission.
  - If an error occurs **during/after** `commit()` (uncertain commit outcome), the application must **never automatically retry** a financial mutation in P0—because blindly retrying after an uncertain commit risks double-debiting or double-crediting. Persistent `idempotency_key` deduplication is planned in **P1** (`F-P1-05`).
- **Audit Event Atomicity (`audit_events` vs. Account/Permission Mutations):**
  - Successful privileged mutations (initial admin bootstrap, staff creation/deactivation, P1 freeze/unfreeze approval) write their `audit_events` row **inside the same JDBC transaction** (`INV-10`), ensuring state changes never commit without an actor-linked audit row.
  - **Explicitly Documented Risk (`RISK-06`):** Because SQLite rolls back the entire transaction on failure, denied or failed attempts do not persist into `audit_events` via the rolled-back transaction; instead, rejected/unauthorized attempts are recorded through `java.util.logging` unless an independent post-rollback audit write is added in P1.

---

## 6. Cross-Reference to Design Invariants

This transaction model directly underpins `INV-01` through `INV-04`, `INV-07` through `INV-09`, `INV-11`, and `INV-12` defined in [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md#4-financial--security-invariants-inv-01-through-inv-12).
