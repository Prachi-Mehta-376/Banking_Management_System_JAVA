# Architecture Decision Records (`DECISIONS.md`)

> **Governance Rule:** No decision is `ACCEPTED` until reviewed and agreed by teammates **S**, **P**, and **R**. Approval dates must never be fabricated; unapproved items remain `PROPOSED` or `PROVISIONAL`.

---

## 1. Decision Record Template (For Future Entries)

```markdown
### ADR-XXX: <Concise Decision Title>
- **Status:** PROPOSED | PROVISIONAL | ACCEPTED | SUPERSEDED | DEPRECATED
- **Date:** Pending Team Approval (record YYYY-MM-DD when accepted)
- **Context:** What architectural problem or constraint requires a choice?
- **Alternatives:** Viable options considered.
- **Decision:** The chosen approach.
- **Rationale:** Why this choice fits UCS553 goals, reliability, and maintainability.
- **Consequences:** Trade-offs, constraints, and follow-up obligations.
- **Related Documents:** Relative links to affected files.
```

---

## 2. Initial Decision Register

### `ADR-001`: Core Technology Stack Alignment with UCS553
- **Status:** `PROVISIONAL`
- **Date:** Pending Team Approval
- **Context:** The project must implement a desktop Banking Management System that aligns closely with the **UCS553 — Enterprise Web Application** syllabus without introducing complex external enterprise frameworks.
- **Alternatives:**
  1. Core Java 17 + JavaFX + Plain JDBC + SQLite + Maven.
  2. Spring Boot + Hibernate/JPA + web frontend (React/HTML).
  3. Swing + serialized flat files (no relational database).
- **Decision:** Use **Java 17, JavaFX, Plain JDBC, SQLite, and Maven**, utilizing standard JDK APIs (Collections, Generics, Custom Exceptions, Java I/O, `java.util.logging`).
- **Rationale:** Directly demonstrates core enterprise Java concepts taught in UCS553 (JDBC transaction management, DAO pattern, layered OOP, desktop GUI) with zero server deployment overhead.
- **Consequences:** Requires manual dependency wiring (`bootstrap/`) and explicit JDBC SQL/transaction boilerplate rather than framework auto-configuration.
- **Related Documents:** [00_MASTER_PROJECT_CONTEXT.md](00_MASTER_PROJECT_CONTEXT.md), [TECHNICAL_CONTEXT.md](TECHNICAL_CONTEXT.md).

---

### `ADR-002`: Strict Three-Tier Layered Architecture
- **Status:** `PROVISIONAL`
- **Date:** Pending Team Approval
- **Context:** Three teammates (`S`, `P`, `R`) need clear architectural seams so UI, business logic, and SQL persistence can evolve cleanly without tight coupling.
- **Alternatives:**
  1. Strict unidirectional three-tier architecture (`ui` -> `service` -> `dao` -> `db`).
  2. JavaFX controllers executing JDBC queries directly (Smart UI anti-pattern).
- **Decision:** Enforce strict unidirectional dependencies: `ui.*` communicates solely with `service.*` via `dto.*` / domain models; `service.*` communicates with persistence solely through `dao.*` interfaces.
- **Rationale:** Enables parallel development, unit testing of business rules without JavaFX or SQLite UI coupling, and clear ownership boundaries.
- **Consequences:** Requires well-defined DTOs and service interfaces before parallel coding accelerates in M2/M3.
- **Related Documents:** [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md), [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

---

### `ADR-003`: Monetary Representation & Precision Strategy
- **Status:** `PROPOSED`
- **Date:** Pending Team Approval
- **Context:** Financial systems cannot tolerate IEEE-754 floating-point rounding inaccuracies (`float`/`double`), and SQLite lacks a native fixed-point `DECIMAL` storage class (converting numeric values to `REAL`/`INTEGER`/`TEXT` by affinity).
- **Alternatives:**
  1. `BigDecimal` in Java domain/DTOs mapped to `INTEGER` minor units (`paise`, i.e., `long` = `amount * 100`) in SQLite.
  2. `BigDecimal` in Java mapped to canonical fixed-scale `TEXT` in SQLite.
  3. `double` in Java and `REAL` in SQLite (rejected).
- **Decision:** Strictly prohibit `float`/`double`/`REAL` for monetary values. Propose **`BigDecimal` (scale 2)** in Java paired with **integer minor units (`paise`)** in SQLite (pending final `S` + `P` confirmation during M1).
- **Rationale:** Integer minor units in SQLite allow exact `SUM()` reconciliation and `CHECK (balance_paise >= 0)` SQL constraints without floating-point drift, while `BigDecimal` provides clean Java formatting and precision arithmetic.
- **Consequences:** DAO mappers must deterministically convert between minor-unit `long` columns and `BigDecimal` amounts.
- **Related Documents:** [TECHNICAL_CONTEXT.md](TECHNICAL_CONTEXT.md), [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

---

### `ADR-004`: Service-Boundary Database Transaction Coordination
- **Status:** `PROPOSED`
- **Date:** Pending Team Approval
- **Context:** Fund transfers require multiple SQL mutations (validate/lock accounts, update source balance, update target balance, record ledger entry, record audit entry) that must succeed or fail atomically.
- **Alternatives:**
  1. Explicit transaction coordinator (`db/` transaction manager / thread-scoped or callback-managed `Connection` with `autoCommit = false`) invoked by services (`S`) and honored by DAOs (`P`).
  2. Auto-commit per DAO method call (rejected — unsafe for transfers).
  3. Placing multi-table transfer SQL inside a single monolithic DAO method (blurs business logic into persistence).
- **Decision:** Coordinate transactions at the **Business Service boundary** using an explicit transaction abstraction provided by `db/` (`P`), without exposing raw JDBC setup boilerplate throughout UI or unrelated services.
- **Rationale:** Keeps business orchestration in `service/` (`S`) while keeping SQLite `Connection` lifecycle and rollback handling cleanly encapsulated in `db/` (`P`).
- **Consequences:** `S` and `P` must finalize the exact transaction helper contract in M1 (`RISK-01`).
- **Related Documents:** [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

---

### `ADR-005`: Three-Role RBAC & Service-Layer Ownership Enforcement
- **Status:** `PROPOSED`
- **Date:** Pending Team Approval
- **Context:** The application serves `Customer`, `Bank Staff`, and `Administrator` roles with distinct privileges and sensitive balance/account operations.
- **Alternatives:**
  1. UI-only menu hiding based on logged-in role.
  2. Mandatory service-layer RBAC + customer-account ownership verification backed by a session security context (`security/`), complemented by UI role routing.
- **Decision:** Enforce both **role permissions** and **customer-to-account ownership checks** inside the Service/Security layer (`S`), treating JavaFX UI gating solely as a UX convenience.
- **Rationale:** Prevents privilege escalation or accidental data leakage across customer accounts regardless of how UI views invoke services.
- **Consequences:** Services require access to the active authenticated principal/session context (`SecurityContext` or explicit principal argument, to be finalized in M1).
- **Related Documents:** [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md), [../AGENTS.md](../AGENTS.md).

---

### `ADR-006`: Equal Peer Module Ownership & Contract-First Collaboration
- **Status:** `PROVISIONAL`
- **Date:** Pending Team Approval
- **Context:** Three teammates (`S`, `P`, `R`) collaborate asynchronously across Git branches on a shared repository.
- **Alternatives:**
  1. Ad-hoc shared ownership of every file (high merge-conflict risk).
  2. Defined primary module ownership (`S`: Engine/Security/Integration, `P`: DB/JDBC, `R`: JavaFX UI) with shared review on cross-module contracts.
- **Decision:** Assign clear primary package ownership across `S`, `P`, and `R` as equal peers (no designated lead), integrating via agreed M1 contracts and cross-reviewed PRs.
- **Rationale:** Minimizes merge conflicts, clarifies accountability for academic evaluation, and allows `R` to build JavaFX screens against agreed contracts/sample providers while `S` and `P` build backend components.
- **Consequences:** Changes to `model/`, `dto/`, or `service/` interfaces require cross-team notice and contract documentation updates.
- **Related Documents:** [../docs/team_handoffs/01_S_BANKING_ENGINE.md](../docs/team_handoffs/01_S_BANKING_ENGINE.md), [02_P_DATABASE_AND_JDBC.md](../docs/team_handoffs/02_P_DATABASE_AND_JDBC.md), [03_R_JAVAFX_AND_UX.md](../docs/team_handoffs/03_R_JAVAFX_AND_UX.md).

---

### `ADR-007`: Immutable Transaction Ledger Records
- **Status:** `PROPOSED`
- **Date:** Pending Team Approval
- **Context:** Simply updating an `accounts.balance` column loses traceability, prevents reconciliation, and invites silent accounting discrepancies.
- **Alternatives:**
  1. Update account balance column without mandatory immutable transaction rows.
  2. Treat transaction/ledger rows as append-only immutable records paired with balance updates inside the same atomic DB transaction, supporting balance reconciliation.
- **Decision:** Require every balance-changing operation (deposit, withdrawal, transfer) to write immutable transaction record(s) atomically alongside the account balance update, and prohibit `UPDATE`/`DELETE` on historical transaction records.
- **Rationale:** Enables customer transaction history, administrative balance reconciliation (P1), and academic explainability.
- **Consequences:** Reversals or disputes (if implemented in P2) must create compensating transactions rather than deleting history.
- **Related Documents:** [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md).
