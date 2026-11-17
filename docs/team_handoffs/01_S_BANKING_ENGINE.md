# Team Handoff — Member S: Banking Engine, Security & Integration (`01_S_BANKING_ENGINE.md`)

> **Peer Role:** Equal Module Owner (**S**) — No hierarchical team lead.
> **Scope:** Business Services, Security/RBAC, Financial Rules, Validation, Exceptions, End-to-End Wiring & Service/Integration Testing.
> **Current Status:** `NOT STARTED` (M0 Documentation & Scaffolding Phase)

---

## 1. Module Purpose

The **Banking Engine, Security & Integration** module is the authoritative business core of the application. It translates UI requests from **R** into validated, authorized, and transactionally consistent operations executed against **P**'s persistence interfaces, ensuring financial invariants and role boundaries can never be bypassed.

---

## 2. Responsibilities

- Design and implement core domain business services (`AuthService`, `CustomerService`, `AccountService`, `TransactionService`, and P1 governance/audit/export services).
- Implement authentication security: salted password hashing, login verification, active session context, and RBAC + customer-account ownership guards (`ADR-005`).
- Enforce financial business rules: positive amounts (`BigDecimal`), non-negative balance validation, active-account checks, atomic transfer orchestration (`ADR-004`), and immutable ledger rule enforcement (`ADR-007`).
- Define custom domain, authorization, validation, and wrapped persistence exception hierarchy (`exception/`).
- Coordinate shared domain models (`model/` with **P**) and service DTOs (`dto/` with **R**).
- Assemble application dependency wiring (`bootstrap/`) connecting **P**'s DAOs, **S**'s services, and **R**'s JavaFX root stage.
- Implement Java I/O statement export helpers and `java.util.logging` setup (`util/`).

---

## 3. Owned Directories

| Directory | Ownership Type | Notes |
| :--- | :---: | :--- |
| `src/main/java/com/tiet/banking/service/` | **Primary Owner** | All business service interfaces and implementations |
| `src/main/java/com/tiet/banking/security/` | **Primary Owner** | Password hashing, session principal context, RBAC/ownership checks |
| `src/main/java/com/tiet/banking/exception/` | **Primary Owner** | Custom exception hierarchy |
| `src/main/java/com/tiet/banking/bootstrap/` | **Primary (Integrated)** | Startup assembly wiring **P**, **S**, and **R** |
| `src/main/java/com/tiet/banking/util/` | **Primary (Shared)** | Shared validation, Java I/O export & logging configuration |
| `src/main/java/com/tiet/banking/model/` | **Co-owned w/ P** | Domain entities & enums agreed jointly in M1 |
| `src/main/java/com/tiet/banking/dto/` | **Co-owned w/ R** | Service request/response DTOs agreed jointly in M1 |
| `src/test/java/com/tiet/banking/service/` | **Primary Owner** | Isolated business logic & RBAC unit tests |
| `src/test/java/com/tiet/banking/integration/` | **Primary (w/ P)** | End-to-end Service + SQLite DAO integration tests |

---

## 4. Dependencies on Other Modules

- **Depends on P (`db/`, `dao/`):** Requires DAO interfaces, transaction coordination mechanism (`ADR-004`, `RISK-01`), and clean persistence exception wrapping.
- **Collaborates with R (`ui/`):** Must freeze service interface contracts and DTO schemas in M1 so **R** can develop JavaFX screens against agreed contracts or mock providers without blocking.

---

## 5. Expected Interfaces (Provisional — Pending M1 Freeze)

- **Exposed to R:** Conceptual service interfaces documented in [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md) (`AuthService`, `CustomerService`, `AccountService`, `TransactionService`, P1 governance/audit services), accepting and returning DTOs/models and throwing custom domain/auth exceptions.
- **Consumed from P:** DAO interfaces (`UserDao`, `CustomerDao`, `AccountDao`, `TransactionDao`, `FreezeRequestDao`, `AuditLogDao`) and the Transaction Coordinator contract (`db/`).

---

## 6. Integration Boundaries

- **Upward Boundary (w/ R):** Never import `javafx.*` classes inside `service/`, `security/`, `model/`, or `dto/`. Keep services completely headless and UI-agnostic.
- **Downward Boundary (w/ P):** Never embed raw SQL strings, `DriverManager`, or `ResultSet` parsing inside `service/`. Delegate all SQLite queries to `dao.*` interfaces.
- **Contract Change Rule:** Any edit to `service/` interfaces, `dto/`, or `model/` requires PR review by **P** and **R** and updates to [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md).

---

## 7. Initial Tasks (For M1 -> M2 Transition)

1. Review Gemini Deep Research findings ([../research/RESEARCH_FINDINGS.md](../research/RESEARCH_FINDINGS.md)) for impact on ledger rules, maker-checker governance, and optional P2 features.
2. Co-approve `ADR-001` through `ADR-007` in [../../context/DECISIONS.md](../../context/DECISIONS.md), resolving `RISK-01` (transaction coordination), `RISK-04` (opening deposit rules), and `RISK-05` (account states).
3. Finalize `model/`, `dto/`, `exception/`, and `service/` interface specifications in M1 before implementation branches open.
4. On branch `feature/banking-engine` (in M2), implement password hashing, RBAC guards, core services, and unit tests.

---

## 8. Acceptance Criteria

- All P0 service operations enforce strict role checks and customer-account ownership rules (`ROLE_PERMISSIONS.md`).
- Monetary calculations exclusively use `BigDecimal` (zero `float`/`double`).
- Transfers and cash operations coordinate atomically through **P**'s transaction boundary with full rollback on error.
- Services operate cleanly headless in JUnit tests and integrate cleanly with **R**'s JavaFX screens via `bootstrap/`.

---

## 9. Testing Responsibilities

- **Service Unit Tests (`src/test/java/com/tiet/banking/service/`):** Test all validation paths, insufficient funds, frozen account rejection, RBAC violations, and customer ownership mismatches using stub/in-memory DAOs (`TC-AUTH-*`, `TC-TXN-*` in [../testing/TEST_PLAN.md](../testing/TEST_PLAN.md)).
- **Integration Tests (`src/test/java/com/tiet/banking/integration/`):** Coordinate with **P** to verify real SQLite transaction commit/rollback behavior (`TC-DB-04`, `TC-P1-01`).

---

## 10. What Is Explicitly Outside This Module's Ownership

- Writing SQLite DDL schemas, PRAGMA configuration, JDBC `PreparedStatement` SQL, or `ResultSet` mappers (owned by **P**).
- Designing or styling JavaFX views, FXML layouts, CSS stylesheets, or UI navigation state (owned by **R**).
- Introducing Spring Boot, Hibernate, or external frameworks outside UCS553.

---

## 11. Current Status

- **Status:** `NOT STARTED`
- **Artifacts Existing:** Directory `.gitkeep` placeholders only (`service/`, `security/`, `exception/`, `bootstrap/`, `model/`, `dto/`, `util/`, `test/.../service/`, `test/.../integration/`).
- **Tests Executed:** `0` (`NOT IMPLEMENTED`).

---

## 12. Resume Instructions

1. Read [../../AGENTS.md](../../AGENTS.md), [../../context/00_MASTER_PROJECT_CONTEXT.md](../../context/00_MASTER_PROJECT_CONTEXT.md), and [../../context/CURRENT_STATUS.md](../../context/CURRENT_STATUS.md).
2. Inspect [../../context/DECISIONS.md](../../context/DECISIONS.md) (`ADR-003`, `ADR-004`, `ADR-005`, `ADR-007`) and [../../context/KNOWN_ISSUES.md](../../context/KNOWN_ISSUES.md) (`RISK-01`, `RISK-02`, `RISK-04`, `RISK-05`).
3. Coordinate with **P** and **R** on M1 contract freeze before writing Java code on branch `feature/banking-engine`.
