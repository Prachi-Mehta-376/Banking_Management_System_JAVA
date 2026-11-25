# Banking Management System (UCS553 — Enterprise Web Application)

> **Project Status:** **Implemented & Verified Baseline (`P0` + `P1` Complete; `P2` Deferred)**
> Built with Java 17, JavaFX 17, Plain JDBC, SQLite, Maven, and JUnit 5. All 12 automated unit, persistence, integration, and live JavaFX GUI tests pass (`0` failures, `0` skipped).

---

## 1. Academic Context & Overview

- **Course:** UCS553 — Enterprise Web Application
- **Institution:** Thapar Institute of Engineering and Technology, Patiala
- **Project Type:** Core Java 17 Desktop Application with JavaFX, Plain JDBC, and SQLite
- **Team Ownership Scope:** **S** (Banking Engine, Security & Integration), **P** (Database, JDBC & Persistence), **R** (JavaFX Presentation & UX)

The **Banking Management System** is an educational desktop banking simulation using fictitious customers and simulated funds. It demonstrates layered Core Java architecture, exact integer-paise arithmetic, atomic balance-plus-transaction-journal persistence, service-layer RBAC, and the **Banking Integrity Control Center** (balance reconciliation, isolated atomic rollback verification, maker-checker account freeze controls, and audit logging).

---

## 2. Implemented Features

### P0 — Working Banking Application (`IMPLEMENTED & VERIFIED`)
- **First-Run Administrator Setup:** One-time bootstrap accepting a user-supplied password (salted `PBKDF2WithHmacSHA256`), atomically locked once an admin exists, with zero hardcoded default credentials.
- **Public Customer Registration:** Creates a `CUSTOMER` identity and one `ACTIVE` bank account (`AC000001` format) starting at `₹0.00` atomically.
- **Three-Role Authentication & RBAC:** Enforced in business services across `CUSTOMER`, `STAFF`, and `ADMIN` (non-teller admin boundary + read-only admin customer lookup).
- **Bank Staff Operations:** Customer/account search, account inspection, simulated cash deposit & withdrawal with receipts.
- **Customer Banking:** Own account overview, internal transfers with zero-sum journal entries, chronological transaction history (`TableView`).
- **Persistence & Atomicity:** 7-table SQLite schema (`PRAGMA foreign_keys = ON`), plain JDBC DAOs (`PreparedStatement`), `TransactionExecutor` + `BankingUnitOfWork` rollback on failure, and `CommitOutcomeUncertainException` handling.

### P1 — Banking Integrity Control Center & Governance (`IMPLEMENTED & VERIFIED`)
- **Balance Reconciliation (`ReconciliationService`):** Non-destructive check comparing stored `balance_paise` against `SUM(transactions.delta_paise)`, verifying running `balance_after_paise` and transfer zero-sum invariants.
- **Isolated Rollback Proof Sandbox:** Demonstrates mid-transfer failure rollback on an isolated temporary SQLite database without touching live data.
- **Maker-Checker Account Control (`AccountControlService`):** Staff freeze/unfreeze requests reviewed by a different Admin; `FROZEN` accounts remain viewable while rejecting deposits, withdrawals, and transfers.
- **Audit Trail (`AuditEventDao` + Admin Viewer):** Same-transaction audit logging for sensitive state changes; rejected attempts logged via `java.util.logging`.
- **CSV Statement Export (`CsvStatementExporter`):** Java I/O export with RFC-4180 quoting and formula-injection neutralization.
- **Persistent Idempotency (`IdempotencyDao`):** Duplicate-request protection across financial operations with conflict detection.

### P2 — Optional Extensions (`DEFERRED`)
- Explainable fraud alerts, transaction disputes, and multiple account types remain deferred to protect P0/P1 reliability.

---

## 3. Build, Test & Run Instructions

**Prerequisites:** JDK 17 (Temurin 17 verified on macOS `arm64`) and Apache Maven 3.10+.

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

# Compile and run all 12 unit, persistence, integration, and live JavaFX tests
mvn clean test

# Launch the desktop JavaFX application (creates runtime DB at data/banking.db)
mvn javafx:run
```

---

## 4. Repository Structure

```text
Banking_Management_System_JAVA/
├── pom.xml                           # Maven Java 17 + JavaFX 17 + sqlite-jdbc + JUnit 5
├── README.md
├── AGENTS.md
├── context/                          # Persistent engineering context & session logs
├── docs/
│   ├── architecture/                 # Layered architecture, DB, contracts, permissions, tx model
│   ├── requirements/                 # SRS & prioritized feature matrix
│   ├── research/                     # External research placeholders
│   ├── testing/                      # Test plan & demo scenarios
│   ├── report/                       # TECHNICAL_REPORT_EVIDENCE.md & 12 live PNG screenshots
│   └── team_handoffs/                # S, P, R module documentation
└── src/
    ├── main/java/com/tiet/banking/   # bootstrap, model, dto, exception, db, dao, security, service, util, ui
    ├── main/resources/               # db/schema.sql & styles/banking.css
    └── test/java/com/tiet/banking/   # unit, persistence, integration & live GUI tests
```

---

## 5. Report Evidence & Navigation

- **Technical Report Evidence & Diagrams:** [docs/report/TECHNICAL_REPORT_EVIDENCE.md](docs/report/TECHNICAL_REPORT_EVIDENCE.md)
- **Live Screenshots:** `docs/report/screenshots/` (`01_first_run_admin_setup.png` – `12_admin_audit_trail.png`)
- **Context & Status:** [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md), [context/IMPLEMENTATION_STATUS.md](context/IMPLEMENTATION_STATUS.md), [context/DECISIONS.md](context/DECISIONS.md), [context/SESSION_LOG.md](context/SESSION_LOG.md)
- **Architecture & Tests:** [docs/architecture/SYSTEM_ARCHITECTURE.md](docs/architecture/SYSTEM_ARCHITECTURE.md), [docs/testing/TEST_PLAN.md](docs/testing/TEST_PLAN.md), [docs/testing/DEMO_SCENARIOS.md](docs/testing/DEMO_SCENARIOS.md)

---

## 6. Academic Simulation Disclaimer

This project is an **educational simulation** created for **UCS553 — Enterprise Web Application** (TIET, Patiala). It uses synthetic data and simulated money only and makes no claim of RBI regulatory compliance or production readiness.
