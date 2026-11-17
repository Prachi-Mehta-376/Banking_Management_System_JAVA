# Feature Matrix & Dependency Map (`FEATURE_MATRIX.md`)

> **Version:** M1 v2.0 Specification (`PROPOSED — AWAITING TEAM REVIEW`)
> **Truthfulness Notice:** Every feature below starts as **`NOT STARTED`**. Priorities represent planned delivery tiers for a three-student academic team.

---

## 1. Hidden Dependency Check (M1 Audit Resolution)

During M1 reconciliation, one potential hidden dependency was identified and explicitly resolved:
- **Audit Logging Dependency (`INV-10`):** P0 requirements (`AuthService.setupInitialAdmin`, `AdminService.createStaffUser`, `AdminService.setStaffActiveStatus`) must emit actor-linked `audit_events` records. Therefore, the **`audit_events` SQLite table and `AuditEventDao.insert`** are placed in **P0 persistence**, while the **Admin Audit Viewer screen and query filters (`F-P1-02`)** remain **P1**.
- **Account Status Guard (`INV-07`):** P0 financial operations enforce `status == ACTIVE` even though the Staff/Admin freeze-unfreeze approval workflow (`F-P1-03`) is P1, ensuring `accounts.status` checks are built cleanly from day one.

---

## 2. Prioritized Feature Matrix

### P0 — Working Banking Application

| ID | Feature | Target Role(s) | Primary Owner(s) | Dependencies | Current Status |
| :---: | :--- | :--- | :---: | :--- | :---: |
| `F-P0-01` | First-run administrator setup (no hardcoded defaults) | Unauthenticated (1st run) | `S` + `P` + `R` | `UserDao`, `AuditEventDao.insert`, Setup UI | `NOT STARTED` |
| `F-P0-02` | Public customer registration & atomic zero-balance account creation | Unauthenticated -> Customer | `S` + `P` + `R` | `UserDao`, `AccountDao`, `TransactionExecutor` | `NOT STARTED` |
| `F-P0-03` | Three-role authentication (`CUSTOMER`, `STAFF`, `ADMIN`) & logout | All Roles | `S` + `P` + `R` | Password hashing, `UserSession`, Login UI | `NOT STARTED` |
| `F-P0-04` | Service-layer three-role authorization & ownership guards | System | `S` | `SecurityContext`, `ROLE_PERMISSIONS.md` | `NOT STARTED` |
| `F-P0-05` | Administrator staff provisioning & activation/deactivation | Administrator | `S` + `P` + `R` | `AdminService`, `UserDao`, `AuditEventDao` | `NOT STARTED` |
| `F-P0-06` | Administrative dashboard summary metrics | Administrator | `S` + `P` + `R` | `AdminService.getAdministrativeSummary` | `NOT STARTED` |
| `F-P0-07` | Staff customer & account search | Bank Staff | `S` + `P` + `R` | `StaffService`, `UserDao`/`AccountDao` | `NOT STARTED` |
| `F-P0-08` | Staff-operated simulated cash deposit | Bank Staff | `S` + `P` + `R` | `TellerService`, `TransactionExecutor`, `OperationDao`, `TransactionDao` | `NOT STARTED` |
| `F-P0-09` | Staff-operated simulated cash withdrawal | Bank Staff | `S` + `P` + `R` | `TellerService`, conditional SQL debit, `INV-01` | `NOT STARTED` |
| `F-P0-10` | Customer balance enquiry (own single account) | Customer | `S` + `P` + `R` | `CustomerBankingService`, session `userId` guard | `NOT STARTED` |
| `F-P0-11` | Customer internal account transfer (atomic debit + credit) | Customer | `S` + `P` + `R` | `CustomerBankingService`, `TransactionExecutor`, `INV-04` | `NOT STARTED` |
| `F-P0-12` | Customer & Staff transaction history view | Customer, Bank Staff | `S` + `P` + `R` | `TransactionDao`, `TransactionView`, JavaFX `TableView` | `NOT STARTED` |
| `F-P0-13` | Persistent SQLite storage & 5-table schema (`PRAGMA foreign_keys = ON`) | System | `P` | `DATABASE_DESIGN.md`, `db/` | `NOT STARTED` |
| `F-P0-14` | Atomic JDBC operations (`TransactionExecutor` & `BankingUnitOfWork`) | System | `P` + `S` | `TRANSACTION_MODEL.md`, `ADR-004` | `NOT STARTED` |
| `F-P0-15` | JavaFX dashboards & styling for Customer, Staff, and Admin workflows | All Roles | `R` (w/ `S` DTOs) | `SERVICE_CONTRACTS.md` | `NOT STARTED` |
| `F-P0-16` | Input validation (`Money` paise parsing) & custom exceptions | System | `S` | `exception/`, `ADR-003` | `NOT STARTED` |
| `F-P0-17` | Automated service & SQLite database tests | System | `S` + `P` | `TEST_PLAN.md` (`TC-01`–`TC-14`) | `NOT STARTED` |

### P1 — Differentiating Controls

| ID | Feature | Target Role(s) | Primary Owner(s) | Dependencies | Current Status |
| :---: | :--- | :--- | :---: | :--- | :---: |
| `F-P1-01` | Non-destructive balance reconciliation (`stored` vs `SUM(delta)`) | Administrator | `S` + `P` + `R` | `ReconciliationService`, `TransactionDao`, `INV-11` | `NOT STARTED` |
| `F-P1-02` | Administrative audit viewer UI & filtered event query | Administrator | `S` + `P` + `R` | `AuditEventDao` (P0 table), `AdminService.getAuditEvents` | `NOT STARTED` |
| `F-P1-03` | Account freeze/unfreeze with maker-checker approval | Bank Staff -> Admin | `S` + `P` + `R` | `account_action_requests` table, `AccountControlService` | `NOT STARTED` |
| `F-P1-04` | CSV statement export (Java I/O) | Customer | `S` + `R` | `CustomerBankingService`, `F-P0-12` | `NOT STARTED` |
| `F-P1-05` | Persistent idempotency protection (`idempotency_key`) | Customer, Bank Staff | `S` + `P` | `operations.idempotency_key` column, `OperationDao` | `NOT STARTED` |

### P2 — Optional Research-Dependent Features

| ID | Feature | Target Role(s) | Primary Owner(s) | Dependencies | Current Status |
| :---: | :--- | :--- | :---: | :--- | :---: |
| `F-P2-01` | Explainable rule-based fraud alerts | Staff, Admin | `S` + `P` + `R` | Pending Gemini Deep Research review + P0/P1 stable | `NOT STARTED` |
| `F-P2-02` | Transaction dispute workflow | Customer, Staff, Admin | `S` + `P` + `R` | Pending Gemini Deep Research review + P1 stable | `NOT STARTED` |
| `F-P2-03` | Multiple account types (`Savings` / `Current` variants) | Customer, Staff | `S` + `P` + `R` | Relaxing v1 1-account/user rule post-M1 | `NOT STARTED` |
| `F-P2-04` | Additional controls justified by verified research | TBD | Shared | Pending Gemini Deep Research review (`RESEARCH_FINDINGS.md`) | `NOT STARTED` |
