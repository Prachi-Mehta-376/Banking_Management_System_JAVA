# Team Handoff — Member S: Banking Engine, Security & Integration (`01_S_BANKING_ENGINE.md`)

> **Peer Role:** Equal Module Owner (**S**) — No hierarchical team lead.
> **Scope:** Core Domain Behavior, Business Services, Auth & RBAC, Financial Invariants, Validation & Exceptions, Application Integration & Service/Integration Testing.
> **Current Status:** `NOT STARTED` (M1 Architecture & Contract Specification Under Review)

---

## 1. Module Purpose

The **Banking Engine, Security & Integration** module enforces all financial invariants (`INV-01`–`INV-12`), role-based access control, validation, and balance-plus-journal orchestration. It coordinates atomic database work through **P**'s `TransactionExecutor` and exposes clean DTO-based service contracts to **R**'s JavaFX screens.

---

## 2. Responsibilities & M1 / M2 Deliverables

### M1 Deliverables (Current Milestone — Documentation Only)
- Define and review domain models (`User`, `BankAccount`, `BankOperation`, `BankTransaction`, `AuditEvent`, `Money`, `Role`, `AccountStatus`, `OperationType`) and service DTOs ([../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md)).
- Define `INV-01` through `INV-12` and transaction orchestration rules ([../architecture/SYSTEM_ARCHITECTURE.md](../architecture/SYSTEM_ARCHITECTURE.md), [../architecture/TRANSACTION_MODEL.md](../architecture/TRANSACTION_MODEL.md)).
- Co-review `TransactionExecutor` / `BankingUnitOfWork` boundaries with **P** and UI-facing DTOs with **R**.

### M2 Implementation Tasks (After M1 Sign-Off)
- Implement `Money` (`long` paise + `BigDecimal` boundary parser, overflow checks) and domain entities/enums (`model/`).
- Implement custom exception hierarchy (`exception/`) and `java.util.logging` / Java I/O helpers (`util/`).
- Implement salted `PBKDF2` password hashing, `UserSession` / `SecurityContext`, and service-layer role/ownership guards (`security/`).
- Implement core P0 services (`AuthService`, `CustomerBankingService`, `TellerService`, `StaffService`, `AdminService`) and P1 services (`ReconciliationService`, `AccountControlService`) (`service/`).
- Implement startup dependency assembly (`bootstrap/`) and coordinate startup/navigation wiring with **R**.
- Author unit tests (`test/.../service/`) and joint SQLite integration tests (`test/.../integration/`).

---

## 3. Owned Directories

| Directory | Ownership Type | Notes |
| :--- | :---: | :--- |
| `src/main/java/com/tiet/banking/service/` | **Primary Owner** | `AuthService`, `CustomerBankingService`, `TellerService`, `StaffService`, `AdminService`, P1 services |
| `src/main/java/com/tiet/banking/security/` | **Primary Owner** | Salted password hashing, active session context, RBAC & ownership enforcement |
| `src/main/java/com/tiet/banking/exception/` | **Primary Owner** | Validation, auth, authorization, insufficient-funds, frozen-account & persistence exceptions |
| `src/main/java/com/tiet/banking/bootstrap/` | **Primary (Integrated)** | Manual dependency wiring (`DB -> DAOs -> Services -> UI`) |
| `src/main/java/com/tiet/banking/util/` | **Primary (Shared)** | Input validation, logging config, P1 CSV statement export |
| `src/main/java/com/tiet/banking/model/` | **Co-owned w/ P** | Domain entities, enums & `Money` value class |
| `src/main/java/com/tiet/banking/dto/` | **Co-owned w/ R** | Presentation-facing DTOs |
| `src/test/java/com/tiet/banking/service/` | **Primary Owner** | Headless service & invariant unit tests |
| `src/test/java/com/tiet/banking/integration/` | **Primary (w/ P)** | Real SQLite service-to-DAO integration tests |

---

## 4. Dependencies on Other Modules

- **Coordinates w/ P (`db/`, `dao/`):** Depends on `TransactionExecutor`, `TransactionWork<T>`, `BankingUnitOfWork`, and the 5 core DAOs (`UserDao`, `AccountDao`, `OperationDao`, `TransactionDao`, `AuditEventDao`).
- **Coordinates w/ R (`ui/`):** Must freeze service method signatures and DTO fields in M1 so **R** can implement JavaFX views against stable DTO contracts and mock providers; supports startup/navigation foundation during M3 integration without removing **R**'s ownership of `ui/`.

---

## 5. Expected Interfaces (Provisional — Pending M1 Freeze)

- **Exposed to R:** `AuthService`, `CustomerBankingService`, `TellerService`, `StaffService`, `AdminService`, and P1 `ReconciliationService` / `AccountControlService` documented in [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md).
- **Consumed from P:** `TransactionExecutor` + DAO interfaces via `BankingUnitOfWork`.

---

## 6. Integration Boundaries

- **No UI Coupling:** Never import `javafx.*` inside `service/`, `security/`, `model/`, or `dto/`.
- **No Raw SQL in Services:** Never embed SQL queries, `DriverManager`, or `ResultSet` code in `service/`; execute all writes via `TransactionExecutor` and DAO interfaces.
- **Simple, Idiomatic Java:** Follow the student-level engineering and strict comment guidelines in [../../AGENTS.md](../../AGENTS.md#5-student-level-engineering-code-quality--practical-collaboration).

---

## 7. Review Responsibilities

- Cross-review **P**'s DAO interfaces, SQLite constraints, and `TransactionExecutor` rollback behavior against `INV-01`–`INV-12`.
- Cross-review **R**'s PRs on shared `dto/` changes and verify JavaFX error feedback maps cleanly from service exceptions.

---

## 8. Acceptance Criteria

- First-run admin setup enforces user-supplied passwords and locks once `COUNT(ADMIN) > 0` (`TC-02`).
- Public registration creates `CUSTOMER` + `1` `ACTIVE` account at `0` paise atomically (`TC-01`).
- Every service enforces RBAC (`INV-05`), customer account ownership (`INV-06`), frozen-account blocking (`INV-07`), and `Money` integer-paise invariants (`INV-02`, `INV-12`).
- Transfers produce equal-and-opposite `transactions` deltas (`INV-04`) and roll back cleanly on failure (`TC-10`, `TC-11`).
- `TC-01` through `TC-14` automated service/integration tests execute cleanly and pass in M2/M3.

---

## 9. Testing Responsibilities

- **Service Unit Tests (`test/.../service/`):** Verify validation, RBAC denials, insufficient funds, and transfer math with in-memory/stub DAOs.
- **Integration Tests (`test/.../integration/`):** Jointly verify end-to-end persistence, atomic rollback, concurrent withdrawal safety, and restart persistence with **P**.

---

## 10. What Is Explicitly Outside This Module's Ownership

- Writing SQLite DDL, PRAGMAs, `PreparedStatement` queries, or `ResultSet` mappers (owned by **P**).
- Authoring JavaFX forms, `TableView` layouts, CSS stylesheets, or UI screenshots (owned by **R**).
- Introducing external frameworks outside UCS553.

---

## 11. Current Status

- **Status:** `NOT STARTED`
- **Artifacts Existing:** Directory `.gitkeep` placeholders only; M1 contracts drafted on branch `docs/m1-architecture-contracts` awaiting team sign-off.
- **Tests Executed:** `0` (`NOT EXECUTED`).

---

## 12. Resume Instructions

1. Read [../../AGENTS.md](../../AGENTS.md), [../../context/CURRENT_STATUS.md](../../context/CURRENT_STATUS.md), and [../../context/DECISIONS.md](../../context/DECISIONS.md).
2. Review [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md) and [../architecture/TRANSACTION_MODEL.md](../architecture/TRANSACTION_MODEL.md) with **P** and **R**.
3. Once M1 PR is approved and merged, open branch `feature/banking-engine` (or `feature/project-foundation` with `pom.xml`) for M2 implementation.
