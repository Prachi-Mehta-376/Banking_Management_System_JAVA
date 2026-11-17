# Implementation Status (`IMPLEMENTATION_STATUS.md`)

> **Truthfulness Rule:** Every implementation module below remains **`NOT STARTED`**. Zero Java source files, SQL schemas, JavaFX screens, or `pom.xml` exist yet.

---

## 1. Status Legend

| Status | Meaning |
| :--- | :--- |
| `NOT STARTED` | No code or artifacts exist yet (design/specification only) |
| `IN PROGRESS` | Actively being implemented on a feature branch |
| `IMPLEMENTED` | Code written, compiles cleanly, awaiting comprehensive testing |
| `TESTED` | Automated or documented verification executed and passing |
| `VERIFIED` | Cross-reviewed, verified against acceptance criteria, merged to `main` |
| `BLOCKED` | Cannot proceed due to unresolved dependency, contract, or decision |
| `DEFERRED` | Intentionally postponed beyond current milestone scope |

---

## 2. M1 v2.0 Module & Feature Tracking Matrix

| ID | Module / Feature | Priority | Owner | Dependencies | Verification Criteria | Status |
| :---: | :--- | :---: | :---: | :--- | :--- | :---: |
| **MOD-00** | Maven `pom.xml` & Java 17 / JavaFX / SQLite Setup | `P0` | Shared (`S`+`P`+`R`) | M1 PR approval | `mvn test-compile` succeeds cleanly | `NOT STARTED` |
| **MOD-01** | Core Domain Models, Enums & `Money` Value Class (`model/`) | `P0` | `S` (w/ `P`) | `ADR-003` (paise + `BigDecimal`) | `Money` & entity invariants verified (`TC-08`) | `NOT STARTED` |
| **MOD-02** | Presentation DTOs & Custom Exception Hierarchy (`dto/`, `exception/`) | `P0` | `S` (w/ `R`) | MOD-01, `SERVICE_CONTRACTS.md` | DTO mapping & typed exception coverage verified | `NOT STARTED` |
| **MOD-03** | SQLite Connection, Pragmas & `TransactionExecutor` (`db/`) | `P0` | `P` | `ADR-004`, `TRANSACTION_MODEL.md` | `PRAGMA foreign_keys=ON`, `commit`/`rollback` (`TC-10`, `TC-11`, `TC-12`) | `NOT STARTED` |
| **MOD-04** | SQLite 5-Table Schema Script (`resources/db/`) | `P0` | `P` | `DATABASE_DESIGN.md` | Fresh DB initializes `users`, `accounts`, `operations`, `transactions`, `audit_events` | `NOT STARTED` |
| **MOD-05** | Plain JDBC DAOs (`UserDao`, `AccountDao`, `OperationDao`, `TransactionDao`, `AuditEventDao`) | `P0` | `P` | MOD-01, MOD-03, MOD-04 | PreparedStatements, `ON DELETE RESTRICT`, conditional updates (`TC-05`–`TC-14`) | `NOT STARTED` |
| **MOD-06** | Salted `PBKDF2` Hashing, `UserSession` & Service RBAC (`security/`) | `P0` | `S` | MOD-05 (`UserDao`) | Auth & role/ownership rejections verified (`TC-02`–`TC-04`) | `NOT STARTED` |
| **MOD-07** | `AuthService` (First-Run Admin Setup, Customer Registration, Login) | `P0` | `S` | MOD-03, MOD-05, MOD-06 | `TC-01`, `TC-02`, `TC-03` pass | `NOT STARTED` |
| **MOD-08** | `AdminService` & `StaffService` (Staff Provisioning, Stats, Customer Search) | `P0` | `S` | MOD-05, MOD-06 | `TC-03`, `TC-04` and staff lookup verified | `NOT STARTED` |
| **MOD-09** | `TellerService` & `CustomerBankingService` (Deposits, Withdrawals, Transfers, History) | `P0` | `S` | MOD-03, MOD-05, MOD-06 | `TC-05`–`TC-14` pass (`INV-01`–`INV-09`, `INV-12`) | `NOT STARTED` |
| **MOD-10** | JavaFX Shell, First-Run Admin Setup, Login & Registration UI (`ui/common`, `ui/auth`) | `P0` | `R` | MOD-02, MOD-07 | Startup routing, registration & validation feedback verified | `NOT STARTED` |
| **MOD-11** | JavaFX Customer, Staff & Admin Dashboards + CSS (`ui/{customer,staff,admin}`, `styles/`) | `P0` | `R` | MOD-08, MOD-09, MOD-10 | All P0 workflows (`DEMO-01`–`DEMO-03`) verified | `NOT STARTED` |
| **MOD-12** | `ReconciliationService` & Non-Destructive Admin Check | `P1` | `S` + `P` + `R` | MOD-05, MOD-09 | `TC-16` (`INV-11`) passes | `NOT STARTED` |
| **MOD-13** | Administrative Audit Viewer (`AdminService.getAuditEvents` + UI) | `P1` | `S` + `P` + `R` | MOD-05 (`AuditEventDao`) | Filtered `audit_events` displayed in Admin UI | `NOT STARTED` |
| **MOD-14** | Account Freeze/Unfreeze Maker-Checker (`account_action_requests`, `AccountControlService`) | `P1` | `S` + `P` + `R` | MOD-05, MOD-09 | `TC-15` (`INV-07`, `INV-10`) passes | `NOT STARTED` |
| **MOD-15** | CSV Statement Export (`Java I/O`) & Persistent Idempotency (`idempotency_key`) | `P1` | `S` + `P` + `R` | MOD-09 | Valid CSV output & `TC-17` idempotent retry pass | `NOT STARTED` |
| **MOD-16** | `java.util.logging`, Startup Wiring (`bootstrap/`) & Integration Suite | `P0` | `S` (Shared) | MOD-00–MOD-11 | `TC-10`–`TC-14` integration & restart tests pass | `NOT STARTED` |
| **MOD-17** | P2 Research Extensions (Fraud Alerts, Disputes, Multi-Account Types) | `P2` | TBD | Gemini Deep Research review | Scoped only after P0/P1 & research review | `NOT STARTED` |
