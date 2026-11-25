# Implementation Status (`IMPLEMENTATION_STATUS.md`)

> **Last Verified:** 2026-11-25 via `mvn clean test` (`12/12` passing) and `mvn -q javafx:run` smoke launch.

---

## 1. Status Legend

| Status | Meaning |
| :--- | :--- |
| `IMPLEMENTED & VERIFIED` | Source code implemented, compiled, and verified by passing tests |
| `DEFERRED` | Intentionally postponed beyond submission baseline (`P2`) |

---

## 2. Module & Feature Implementation Matrix

| ID | Module / Feature | Priority | Planned Owner | Implemented Artifacts | Status |
| :---: | :--- | :---: | :---: | :--- | :---: |
| **MOD-00** | Maven `pom.xml` & Java 17 / JavaFX / SQLite Setup | `P0` | Shared | `pom.xml` | `IMPLEMENTED & VERIFIED` |
| **MOD-01** | Core Domain Models, Enums & `Money` (`model/`) | `P0` | `S` (w/ `P`) | 5 enums, `Money`, 7 records (`MoneyValidationTest`) | `IMPLEMENTED & VERIFIED` |
| **MOD-02** | DTOs, Exceptions & Utils (`dto/`, `exception/`, `util/`) | `P0` | `S` (w/ `R`) | 11 DTOs, 10 exceptions, `AppConfig`, `AppLogger`, `InputValidator` | `IMPLEMENTED & VERIFIED` |
| **MOD-03** | SQLite Connection, Pragmas & `TransactionExecutor` (`db/`) | `P0` | `P` | `DatabaseManager`, `JdbcTransactionExecutor`, `BankingUnitOfWork` | `IMPLEMENTED & VERIFIED` |
| **MOD-04** | SQLite Schema (`resources/db/schema.sql`) | `P0` | `P` | 7 tables (`users`..`idempotency_records`), constraints & indexes | `IMPLEMENTED & VERIFIED` |
| **MOD-05** | Plain JDBC DAOs (`dao/`, `dao/jdbc/`) | `P0` | `P` | 7 DAO interfaces & 7 `Jdbc*Dao` implementations | `IMPLEMENTED & VERIFIED` |
| **MOD-06** | Salted `PBKDF2` Hashing & `SessionManager` (`security/`) | `P0` | `S` | `PasswordHasher`, `SessionManager` (`AuthAndRbacTest`) | `IMPLEMENTED & VERIFIED` |
| **MOD-07** | `AuthService` (First-Run Admin, Customer Reg, Login/Logout) | `P0` | `S` | `AuthService` (`AuthAndRbacTest`, `LiveGuiVerificationAndScreenshotTest`) | `IMPLEMENTED & VERIFIED` |
| **MOD-08** | `AdminService` & `StaffService` | `P0` | `S` | `AdminService`, `StaffService` | `IMPLEMENTED & VERIFIED` |
| **MOD-09** | `TellerService` & `CustomerBankingService` | `P0` | `S` | `TellerService`, `CustomerBankingService` (`BankingEngineIntegrationTest`) | `IMPLEMENTED & VERIFIED` |
| **MOD-10** | JavaFX Auth Views & Common Shell (`ui/common`, `ui/auth`) | `P0` | `R` | `AppNavigator`, `AppShell`, `UiComponents`, `FirstRunAdminView`, `LoginView`, `CustomerRegistrationView` | `IMPLEMENTED & VERIFIED` |
| **MOD-11** | JavaFX Customer, Staff & Admin Dashboards + CSS | `P0` | `R` | `CustomerDashboardView`, `StaffDashboardView`, `AdminDashboardView`, `banking.css` | `IMPLEMENTED & VERIFIED` |
| **MOD-12** | `ReconciliationService` & Integrity Control Center | `P1` | `S` + `P` + `R` | `ReconciliationService` + UI reconciliation & rollback sandbox | `IMPLEMENTED & VERIFIED` |
| **MOD-13** | Administrative Audit Viewer (`AdminService.getAuditEvents` + UI) | `P1` | `S` + `P` + `R` | `JdbcAuditEventDao.findRecent` + `AdminDashboardView` Audit tab | `IMPLEMENTED & VERIFIED` |
| **MOD-14** | Account Freeze/Unfreeze Maker-Checker (`AccountControlService`) | `P1` | `S` + `P` + `R` | `AccountControlService`, `JdbcAccountActionRequestDao` + Staff/Admin tabs | `IMPLEMENTED & VERIFIED` |
| **MOD-15** | CSV Statement Export & Persistent Idempotency | `P1` | `S` + `P` + `R` | `CsvStatementExporter` (formula-safe), `JdbcIdempotencyDao` | `IMPLEMENTED & VERIFIED` |
| **MOD-16** | Startup Wiring (`bootstrap/`) & Automated + Live GUI Suite | `P0` | `S` | `ServiceRegistry`, `BankingApp`, `Launcher`, 5 JUnit 5 suites (`12/12` pass) | `IMPLEMENTED & VERIFIED` |
| **MOD-17** | P2 Research Extensions (Fraud Alerts, Disputes, Account Types) | `P2` | — | Deferred to protect verified P0/P1 submission baseline | `DEFERRED` |
