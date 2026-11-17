# Verification & Test Plan (`TEST_PLAN.md`)

> **Execution Notice:** **NO APPLICATION CODE OR TESTS EXIST YET.** Every test scenario below has status **`NOT EXECUTED`**. Never mark any test as passing unless it has actually been compiled and executed against implemented code.

---

## 1. Planned Testing Pyramid & Ownership

| Layer | Planned Location | Primary Owner | Test Tooling (Planned) | Current Status |
| :--- | :--- | :---: | :--- | :---: |
| **Unit / Service Tests** | `src/test/java/com/tiet/banking/service/` | `S` | JUnit 5 (with mock/stub DAOs) | `NOT IMPLEMENTED` |
| **DAO / Persistence Tests** | `src/test/java/com/tiet/banking/dao/` | `P` | JUnit 5 + isolated temporary SQLite test DB | `NOT IMPLEMENTED` |
| **End-to-End Integration Tests** | `src/test/java/com/tiet/banking/integration/` | `S` (w/ `P`) | JUnit 5 + real Services + real SQLite DAOs | `NOT IMPLEMENTED` |
| **JavaFX Manual UI Verification** | [DEMO_SCENARIOS.md](DEMO_SCENARIOS.md) + `docs/report/screenshots/` | `R` (w/ `S`, `P`) | Manual checklist across 3 role dashboards | `NOT EXECUTED` |

---

## 2. Proposed Test Scenarios Register (All `NOT EXECUTED`)

### 2.1 Authentication & RBAC Tests (`S` / Integration)

| Test ID | Scenario | Expected Behavior | Status |
| :--- | :--- | :--- | :---: |
| `TC-AUTH-01` | Valid login for Customer, Staff, and Admin | Returns authenticated session with exact role; hashes compared securely | `NOT EXECUTED` |
| `TC-AUTH-02` | Login with incorrect password or unknown username | Throws authentication failure; session remains unauthenticated | `NOT EXECUTED` |
| `TC-AUTH-03` | Customer attempts Staff-only cash deposit service | Service throws authorization exception before DAO access | `NOT EXECUTED` |
| `TC-AUTH-04` | Customer A attempts balance query or transfer from Customer B's account | Service rejects with ownership/authorization exception | `NOT EXECUTED` |
| `TC-AUTH-05` | Staff attempts to approve their own freeze request (P1) | Rejected by service & maker-checker constraint | `NOT EXECUTED` |

### 2.2 Banking Engine & Financial Validation Tests (`S` / Service)

| Test ID | Scenario | Expected Behavior | Status |
| :--- | :--- | :--- | :---: |
| `TC-TXN-01` | Staff cash deposit with positive amount on `ACTIVE` account | Balance increases accurately (`BigDecimal`); immutable deposit row created | `NOT EXECUTED` |
| `TC-TXN-02` | Deposit or transfer with `0.00` or negative amount | Rejected with validation exception; zero DB mutation | `NOT EXECUTED` |
| `TC-TXN-03` | Staff withdrawal exceeding account balance | Rejected with insufficient funds exception; balance unchanged | `NOT EXECUTED` |
| `TC-TXN-04` | Customer internal transfer between two `ACTIVE` accounts with sufficient balance | Source debited, destination credited by exact equal amount; atomic ledger row | `NOT EXECUTED` |
| `TC-TXN-05` | Transfer to same source & destination account | Rejected with validation exception | `NOT EXECUTED` |
| `TC-TXN-06` | Operation involving `FROZEN` or `CLOSED` account | Rejected by account lifecycle guard | `NOT EXECUTED` |

### 2.3 Persistence, Constraints & Atomicity Tests (`P` / DAO & Integration)

| Test ID | Scenario | Expected Behavior | Status |
| :--- | :--- | :--- | :---: |
| `TC-DB-01` | Schema initialization on fresh SQLite file | All tables, foreign keys, and `CHECK` constraints created cleanly | `NOT EXECUTED` |
| `TC-DB-02` | Foreign key enforcement check (`PRAGMA foreign_keys = ON`) | Inserting account for nonexistent customer fails with constraint violation | `NOT EXECUTED` |
| `TC-DB-03` | Negative balance check constraint | Direct negative balance update rejected by SQLite `CHECK` constraint | `NOT EXECUTED` |
| `TC-DB-04` | Simulated failure mid-transfer (after source debit, before commit) | JDBC `rollback()` restores source balance; zero partial ledger rows committed | `NOT EXECUTED` |
| `TC-DB-05` | Balance reconciliation sum check (P1) | Sum of deposits minus withdrawals equals sum of current account balances | `NOT EXECUTED` |

### 2.4 P1 / P2 Functional Verification (`S` + `P` + `R`)

| Test ID | Scenario | Expected Behavior | Status |
| :--- | :--- | :--- | :---: |
| `TC-P1-01` | Staff requests freeze -> Admin approves -> Customer attempts transfer | Transfer denied while `FROZEN`; audit log records request & approval | `NOT EXECUTED` |
| `TC-P1-02` | Statement export via Java I/O | Produces well-formed local export file matching visible `TableView` ledger | `NOT EXECUTED` |
| `TC-P2-01` | Duplicate transfer key / risk alert trigger (if P2 scoped) | Behavior matches approved P2 specification | `NOT EXECUTED` |
