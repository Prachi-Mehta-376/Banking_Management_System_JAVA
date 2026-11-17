# Financial Transaction Model (`TRANSACTION_MODEL.md`)

> **Status:** `PROPOSED SPECIFICATION — PENDING M1 APPROVAL`
> **Related Decisions:** `ADR-003` (Money), `ADR-004` (Atomic Transactions), `ADR-007` (Immutable Ledger).

---

## 1. Core Financial Invariants

1. **No Floating-Point Drift:** All amounts are processed with exact decimal precision (`BigDecimal` scale 2 in Java; proposed `INTEGER` minor units `paise` in SQLite).
2. **Strict Positive Amounts:** Every deposit, withdrawal, and transfer amount must be strictly greater than zero (`amount > 0`).
3. **Non-Negative Account Balances:** Simulated accounts do not support overdrafts in the baseline (`balance >= 0`, enforced in both business logic and SQLite `CHECK` constraints).
4. **Conservation of Simulated Funds on Transfers:** An internal transfer changes the sum of source and destination balances by **zero** (`debit == credit`). Only **Staff Cash Deposit** creates simulated balance in the system, and only **Staff Cash Withdrawal** removes simulated balance.
5. **Atomicity (All-or-Nothing):** Every balance-changing operation executes inside a single explicit JDBC transaction (`autoCommit = false`). Either all balance updates, ledger entries, and transactional audit entries commit together, or everything rolls back.
6. **Immutable Ledger:** Once committed, transaction records cannot be modified or deleted. Corrections or reversals require new compensating entries.

---

## 2. Conceptual Execution Flows (Proposed)

### 2.1 Simulated Cash Deposit (Role: `Bank Staff`)
1. Verify authenticated principal has `STAFF` role.
2. Validate `amount > 0` and non-empty operation note.
3. Begin JDBC transaction (`setAutoCommit(false)`).
4. Load target account; verify account exists and status is `ACTIVE`.
5. Credit target account balance (`balance + amount`).
6. Insert immutable `CASH_DEPOSIT` transaction record.
7. Insert audit log record (`P1`).
8. Commit JDBC transaction (or rollback on any failure).

### 2.2 Simulated Cash Withdrawal (Role: `Bank Staff`)
1. Verify authenticated principal has `STAFF` role.
2. Validate `amount > 0`.
3. Begin JDBC transaction.
4. Load target account; verify account exists and status is `ACTIVE`.
5. Verify `account.balance >= amount`; reject with domain exception (`InsufficientFundsException` concept) if insufficient.
6. Debit target account balance (`balance - amount`).
7. Insert immutable `CASH_WITHDRAWAL` transaction record.
8. Insert audit log record (`P1`).
9. Commit JDBC transaction (or rollback on any failure).

### 2.3 Internal Account-to-Account Transfer (Role: `Customer`)
1. Verify authenticated principal has `CUSTOMER` role.
2. Validate `amount > 0` and `sourceAccountNumber != destinationAccountNumber`.
3. Begin JDBC transaction.
4. Load source account; verify it exists, **belongs to the authenticated customer**, and status is `ACTIVE`.
5. Load destination account; verify it exists and status is `ACTIVE`.
6. Verify `source.balance >= amount`.
7. Debit source account (`source.balance - amount`).
8. Credit destination account (`destination.balance + amount`).
9. Insert immutable `INTERNAL_TRANSFER` transaction record linking source and destination.
10. Insert audit log record (`P1`).
11. Commit JDBC transaction; if **any** step fails (including DB constraint error or exception after step 7), execute `rollback()` so source balance remains untouched.

---

## 3. Account State Eligibility Rules (Open Question `RISK-05`)

| Account Status | Cash Deposit | Cash Withdrawal | Outgoing Transfer | Incoming Transfer | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `ACTIVE` | Allowed | Allowed | Allowed | Allowed | Normal operating state |
| `FROZEN` | **Denied** (Proposed) | **Denied** | **Denied** | **Denied** (Confirm M1) | Awaiting Admin unfreeze |
| `CLOSED` | **Denied** | **Denied** | **Denied** | **Denied** | Terminal state |
| `PENDING` (If used) | **Denied** | **Denied** | **Denied** | **Denied** | Before activation |

---

## 4. P2 Extensions Pending Research Review

- **Idempotency / Duplicate-Request Prevention:** Client-generated request token checked against recent transactions to prevent accidental double-clicks or repeated submissions.
- **Explainable Fraud-Risk Alerts:** Deterministic, inspectable threshold/velocity checks (e.g., high-value single transfer warning or rapid burst flag) surfaced cleanly for staff/admin review—not opaque ML scoring.
