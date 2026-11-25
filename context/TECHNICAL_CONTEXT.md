# Technical Context (`TECHNICAL_CONTEXT.md`)

> **Focus:** Summary of M1 Technical Baseline, Monetary Arithmetic, Transaction Coordination, Invariants & Coding Discipline.
> **Authoritative Contracts:** [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md), [DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

---

## 1. Layer Stack & M1 Simplifications

- **Flow:** `JavaFX UI (ui.*)` -> `Business Services (service.*)` -> `DAO Interfaces (dao.*)` -> `JDBC Implementations (dao.jdbc.*)` -> `SQLite (data/*.db)`.
- **M1 Simplifications:** Unified `users` table (no separate `customers` table in v1), max `1` account per customer (`UNIQUE(user_id)`), `2` account statuses (`ACTIVE`, `FROZEN`), and zero unnecessary frameworks or wrapper layers ([../AGENTS.md](../AGENTS.md#5-student-level-engineering-code-quality--practical-collaboration)).

---

## 2. Monetary Model (`ADR-003`) & Balance-Plus-Journal (`ADR-007`)

- **Persisted & Internal Representation:** `long` integer paise (`INTEGER` in SQLite; `₹1.00 = 100 paise`), guarded against overflow via `Math.addExact`/`Math.subtractExact` (`INV-12`).
- **Boundary Parsing:** `Money` parses user input via `BigDecimal`, rejecting `null`, `<= 0`, `scale > 2`, and overflow (`INV-02`). Zero `float`/`double` usage.
- **Journal Structure:** Each financial operation writes `1` row to `operations` and `1` (`DEPOSIT` `+`, `WITHDRAWAL` `-`) or `2` (`TRANSFER` `-` and `+`, sum = `0`) rows to `transactions` atomically alongside updated `accounts.balance_paise`. This is a simplified simulation model, not a full bank-wide double-entry general ledger.
- **Reconciliation (P1):** Compares `accounts.balance_paise` with `SUM(transactions.delta_paise)` non-destructively (`INV-11`).

---

## 3. Atomic Transactions (`ADR-004`) & SQLite Rules

- **`TransactionExecutor` / `TransactionWork<T>` / `BankingUnitOfWork`:** `P` manages single-connection lifecycle (`PRAGMA foreign_keys = ON`, `autoCommit = false`, `commit`, `rollback`, `SQLITE_BUSY` handling); `S` supplies business validation, auth checks, conditional balance updates (`rowsAffected == 1`), and journal writes.
- **Safety Policies:** No automatic retries on uncertain commit outcomes in P0; `operations.idempotency_key` (`UNIQUE NULL`) supports P1 idempotency (`F-P1-05`).
- **Audit Atomicity (`INV-10`):** Successful admin/staff governance mutations write `audit_events` in the same DB transaction; rejected attempts log via `java.util.logging` (`RISK-06`).

---

## 4. Security, Exceptions, Logging & Planned Build

- **Security (`ADR-005`, `ADR-008`):** Per-user salted `PBKDF2WithHmacSHA256` password hashes (`password_hash`, `password_salt`, `password_algo`), first-run admin bootstrap without hardcoded passwords, and service-layer RBAC + ownership checks via `UserSession`.
- **Exceptions (`exception/`):** Typed validation, auth, authorization, insufficient-funds, frozen-account, conflict, and persistence exceptions.
- **Implemented Invariants:** `INV-01` through `INV-12` in [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md#4-financial--security-invariants-inv-01-through-inv-12) (`IMPLEMENTED & VERIFIED` across `src/test/`).
- **Build & Launch:** Maven + Java 17 (`pom.xml`, plus official Apache Maven Wrapper `mvnw` / `mvnw.cmd`).
