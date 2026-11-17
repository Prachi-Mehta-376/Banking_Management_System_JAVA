# Financial & Transaction Management Model (`TRANSACTION_MODEL.md`)

> **Version:** M1 v2.0 Finalized Contract (`RECOMMENDED — AWAITING PR #1 APPROVAL`)
> **Related Decisions:** `ADR-003` (Integer Paise), `ADR-004` (`TransactionExecutor`), `ADR-007` (Balance-Plus-Journal), `ADR-008` (Bootstrap & Lifecycle).

---

## 1. Monetary Representation (`ADR-003`)

- **Persisted & Internal Unit:** All monetary balances and transaction deltas use **64-bit signed integer paise** (`long` in Java, `INTEGER` in SQLite; `₹1.00 = 100 paise`).
- **Boundary Parsing (`Money`):** User input is parsed via `java.math.BigDecimal`, rejecting:
  - `null`, blank, or non-numeric input
  - Zero or negative amounts (`<= 0`) for financial operations (`INV-02`)
  - More than 2 decimal places (`scale() > 2`)
  - Values exceeding `long` paise bounds (`ArithmeticException` -> `ValidationException`)
- **Arithmetic Safety:** Balance additions and subtractions use `Math.addExact` / `Math.subtractExact` (`INV-12`), never `float` or `double`.

---

## 2. Balance-Plus-Transaction-Journal Model (`ADR-007`)

Every successful financial operation atomically persists:
1. **One** `operations` row with a unique server-generated `reference_id` (`Decision B`).
2. **One or more** signed `transactions` entries (`operation_id`, `account_id`, `delta_paise`, `balance_after_paise`, `created_at`).
3. **Atomic balance updates** to `accounts.balance_paise` for every affected account.

### Operation Examples
- **Simulated Cash Deposit ₹500 (`50000` paise):** `1` `operations` row (`DEPOSIT`, `performed_by = staffUserId`) + `1` `transactions` entry (`+50000`) + target balance `+50000`.
- **Simulated Cash Withdrawal ₹200 (`20000` paise):** `1` `operations` row (`WITHDRAWAL`, `performed_by = staffUserId`) + `1` `transactions` entry (`-20000`) + target balance `-20000`.
- **Internal Transfer ₹1,000 (`100000` paise) Account A -> Account B:** `1` `operations` row (`TRANSFER`, `performed_by = customerAUserId`) + `2` `transactions` entries sharing `operation_id` (`-100000` on A, `+100000` on B; sum of deltas = `0` per `INV-04`) + atomic updates to both balances.

> **Scope Boundary:** Cash deposits and withdrawals represent **simulated external money movements**, not a full bank-wide double-entry general ledger with vault or reserve contra accounts.

---

## 3. Reconciliation Model (P1)

Because all v1 customer accounts initialize with `balance_paise = 0`, P0 tables directly support P1 reconciliation:

```text
expected_balance_paise = SUM(transactions.delta_paise WHERE account_id = ?)
```

`ReconciliationService` (P1) compares `expected_balance_paise` to `accounts.balance_paise`:
- If equal: reports account matched.
- If different: reports the exact mismatch (`storedBalancePaise`, `expectedBalancePaise`, `discrepancyPaise`) and **never** silently edits stored balances or journal rows (`INV-11`), preserving investigative context.
- **Limitation:** A local desktop SQLite file cannot prevent external modification by an OS user with filesystem access (`RISK-03`).

---

## 4. Transaction Management Contract (`ADR-004`)

### 4.1 Core Abstractions & Ownership Split (`com.tiet.banking.db`)
- **`TransactionExecutor`:** `T execute(TransactionWork<T> work)` — opens/manages one SQLite `Connection`, runs `work`, commits on success, rolls back on failure, and distinguishes uncertain commit outcomes.
- **`TransactionWork<T>`:** Functional callback `T execute(BankingUnitOfWork uow)`.
- **`BankingUnitOfWork`:** Exposes DAO handles (`users()`, `accounts()`, `operations()`, `transactions()`, `auditEvents()`, and P1 `accountActionRequests()`) bound to that **exact single JDBC `Connection`**.

| Responsibility | Owner | Scope |
| :--- | :---: | :--- |
| Connection lifecycle, `PRAGMA foreign_keys = ON`, `BEGIN IMMEDIATE` / `setAutoCommit(false)`, `commit()`, `rollback()`, cleanup, `SQLITE_BUSY` timeout & SQL exception mapping | **P** | `db/` & `dao/jdbc/` |
| Authentication, RBAC checks, `Money` validation, business rules, orchestration, journal/audit entries, domain exceptions | **S** | `service/` & `security/` |

### 4.2 Why All Participating DAOs Must Share One Connection
In SQLite + JDBC, transaction boundaries belong strictly to one `java.sql.Connection`. Using separate connections across DAOs inside one operation breaks atomicity (a failed journal insert would leave a committed debit, violating `INV-03`) and triggers immediate `SQLITE_BUSY` write-lock deadlocks between connections.

### 4.3 Mandatory 9-Step Financial Execution Sequence
1. **Authenticate & Authorize:** Verify active `UserSession`, required `Role`, and customer ownership (`INV-05`, `INV-06`).
2. **Validate Input:** Parse `Money` (`> 0`, `<= 2` decimals, overflow-safe) and verify distinct non-blank account numbers (`INV-02`).
3. **Enter Database Transaction:** Call `TransactionExecutor.execute(...)` (acquires 1 `Connection`, applies `PRAGMA foreign_keys = ON`, disables auto-commit, acquires SQLite write lock).
4. **Re-Read Account State Inside Transaction:** Query participating account(s) via `uow.accounts()` inside the active transaction (ordered deterministically by account ID for transfers).
5. **Check Status & Funds:** Reject `FROZEN` accounts (`INV-07`) and insufficient source balance (`INV-01`).
6. **Safely Update Balances:** Execute conditional SQL updates (`debitIfActiveAndSufficient`, `creditIfActive`) with overflow checks (`INV-12`), asserting `rowsAffected == 1`.
7. **Write Operation & Journal Records:** Insert `1` `operations` row with a unique server-generated `reference_id`, followed by `1` or `2` `transactions` rows with exact `delta_paise` and `balance_after_paise` (`INV-08`).
8. **Commit:** Call `connection.commit()` only after every write succeeds and `rowsAffected == 1` assertions pass.
9. **Rollback on Failure:** Any exception before `commit()` triggers `connection.rollback()`, clean resource release, and safe domain/persistence exception propagation (`INV-03`).

---

## 5. Failure Modes, Concurrency & Audit Rules (Decisions B, D, E, F)

- **Affected-Row Verification & Overflow:** `AccountDao` verifies `executeUpdate() == 1` on every conditional update; if `0` rows match (concurrent drain or freeze), abort and roll back. Services check `Math.addExact` before calling `creditIfActive`.
- **SQLite Write-Lock & `SQLITE_BUSY` Handling:** `db/` configures `PRAGMA busy_timeout = 5000` and begins write transactions cleanly (`BEGIN IMMEDIATE` where appropriate) so concurrent withdrawals serialize safely (`TC-12`).
- **Failure Injection for Verification:** Integration tests (`TC-10`, `TC-11`) inject failure inside a test callback/DAO wrapper between balance update and commit without embedding backdoors in production services.
- **Outcome Classification & Uncertain Commit Handling (`Decision E`):**
  1. **Known failure before commit:** Rollback guaranteed; zero committed state; safe error displayed to user.
  2. **Confirmed successful commit:** Returns `TransactionReceipt` with `referenceId`.
  3. **Uncertain commit outcome:** If an I/O or SQL error occurs during `connection.commit()` itself, `TransactionExecutor` throws `CommitOutcomeUncertainException` (preserving the operation `referenceId` when created prior to commit). The application **must never automatically repeat the financial operation**. The UI displays a clear warning instructing the user to inspect Transaction History / `referenceId` before retrying.
  - *Acknowledged Limitation:* Without external distributed coordinators or persistent P1 `idempotency_key` (`Decision B`), a crash or I/O error mid-`commit()` cannot be automatically resolved by a local desktop client.
- **Audit Behavior (`Decision D`):**
  - Sensitive state changes (initial admin setup, staff creation/activation/deactivation, P1 freeze/unfreeze approval) write `audit_events` **inside the same JDBC transaction** (`INV-10`). If that transaction rolls back, no misleading success row remains in `audit_events`.
  - Rejected or failed attempts (invalid login, authorization denial, insufficient funds) are logged via `java.util.logging` **without** passwords, sensitive customer PII, or false database audit rows.
- **First-Run Administrator Concurrency (`Decision F`):**
  - `AuthService.setupInitialAdmin` executes inside `TransactionExecutor`, re-checks `UserDao.countByRole(Role.ADMIN) == 0` under the write transaction, inserts the `ADMIN` user with salted password hash, and inserts `INITIAL_ADMIN_CREATED` into `audit_events` atomically—blocking concurrent or duplicate bootstrap attempts (`ConflictException`).
