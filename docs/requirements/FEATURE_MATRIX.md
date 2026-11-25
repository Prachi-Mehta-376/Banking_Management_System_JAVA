# Feature Matrix & Status (`FEATURE_MATRIX.md`)

> **Last Verified:** 2026-11-25 (`mvn clean test`: `12/12` passing; `mvn -q javafx:run` smoke launch verified).

---

## 1. Prioritized Feature Matrix

### P0 — Working Banking Application

| ID | Feature | Target Role(s) | Status |
| :---: | :--- | :--- | :---: |
| `F-P0-01` | First-run administrator setup (salted PBKDF2, atomic lock, no default credentials) | Unauthenticated (1st run) | `IMPLEMENTED & VERIFIED` |
| `F-P0-02` | Public customer registration & atomic zero-balance account creation (`AC000001`) | Unauthenticated -> Customer | `IMPLEMENTED & VERIFIED` |
| `F-P0-03` | Three-role authentication (`CUSTOMER`, `STAFF`, `ADMIN`) & logout | All Roles | `IMPLEMENTED & VERIFIED` |
| `F-P0-04` | Service-layer three-role authorization & single-account ownership guards | System | `IMPLEMENTED & VERIFIED` |
| `F-P0-05` | Administrator staff provisioning & activation/deactivation | Administrator | `IMPLEMENTED & VERIFIED` |
| `F-P0-06` | Administrative dashboard live metrics (`AdministrativeSummary`) | Administrator | `IMPLEMENTED & VERIFIED` |
| `F-P0-07` | Customer & account search (`STAFF` + read-only `ADMIN`) | Bank Staff, Administrator | `IMPLEMENTED & VERIFIED` |
| `F-P0-08` | Staff-operated simulated cash deposit | Bank Staff | `IMPLEMENTED & VERIFIED` |
| `F-P0-09` | Staff-operated simulated cash withdrawal | Bank Staff | `IMPLEMENTED & VERIFIED` |
| `F-P0-10` | Customer balance enquiry (own single account) | Customer | `IMPLEMENTED & VERIFIED` |
| `F-P0-11` | Customer internal account transfer (atomic debit + credit) | Customer | `IMPLEMENTED & VERIFIED` |
| `F-P0-12` | Customer & Staff transaction history (`TableView`) | Customer, Bank Staff | `IMPLEMENTED & VERIFIED` |
| `F-P0-13` | Persistent SQLite storage (`schema.sql`, `PRAGMA foreign_keys = ON`) | System | `IMPLEMENTED & VERIFIED` |
| `F-P0-14` | Atomic JDBC operations (`JdbcTransactionExecutor` & `BankingUnitOfWork`) | System | `IMPLEMENTED & VERIFIED` |
| `F-P0-15` | JavaFX dashboards & CSS styling for all three roles | All Roles | `IMPLEMENTED & VERIFIED` |
| `F-P0-16` | `Money` integer-paise validation & typed custom exceptions | System | `IMPLEMENTED & VERIFIED` |
| `F-P0-17` | Automated JUnit 5 service, persistence, integration & GUI tests | System | `IMPLEMENTED & VERIFIED` |

### P1 — Differentiating Controls (Banking Integrity Control Center)

| ID | Feature | Target Role(s) | Status |
| :---: | :--- | :--- | :---: |
| `F-P1-01` | Non-destructive balance & journal reconciliation (`ReconciliationService`) | Administrator | `IMPLEMENTED & VERIFIED` |
| `F-P1-02` | Administrative audit trail viewer (`AdminService.getAuditEvents`) | Administrator | `IMPLEMENTED & VERIFIED` |
| `F-P1-03` | Account freeze/unfreeze maker-checker (`AccountControlService`) | Bank Staff -> Admin | `IMPLEMENTED & VERIFIED` |
| `F-P1-04` | Formula-safe CSV statement export (`CsvStatementExporter`) | Customer | `IMPLEMENTED & VERIFIED` |
| `F-P1-05` | Persistent idempotency protection (`idempotency_records`) | Customer, Bank Staff | `IMPLEMENTED & VERIFIED` |

### P2 — Optional Research-Dependent Features

| ID | Feature | Target Role(s) | Status |
| :---: | :--- | :--- | :---: |
| `F-P2-01` | Explainable rule-based fraud alerts | Staff, Admin | `DEFERRED` |
| `F-P2-02` | Transaction dispute workflow | Customer, Staff, Admin | `DEFERRED` |
| `F-P2-03` | Multiple account types (`Savings` / `Current` variants) | Customer, Staff | `DEFERRED` |
| `F-P2-04` | Additional research-justified controls | TBD | `DEFERRED` |
