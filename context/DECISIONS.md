# Architecture Decision Records (`DECISIONS.md`)

> **Governance Rule:** Technical resolution and human sign-off are distinct. Decisions A–F resolved the remaining M1 contract questions technically, so `ADR-001`–`ADR-008` are marked **`RECOMMENDED — AWAITING APPROVAL`**. Do **not** mark any ADR `ACCEPTED` until teammates **S**, **P**, and **R** approve PR #1.

---

## 1. Decision Record Template (For Future Entries)

```markdown
### ADR-XXX: <Concise Decision Title>
- **Status:** PROPOSED | PROVISIONAL | RECOMMENDED — AWAITING APPROVAL | ACCEPTED | SUPERSEDED | DEPRECATED
- **Date:** Pending Team Approval (record YYYY-MM-DD when accepted)
- **Context:** What architectural problem or constraint requires a choice?
- **Alternatives:** Viable options considered.
- **Decision:** The finalized recommended approach.
- **Rationale:** Why this choice fits UCS553 goals, reliability, and maintainability.
- **Consequences & Outstanding Dependencies:** Trade-offs, integration effects, and any remaining external/team gates.
- **Related Documents:** Relative links to affected files.
```

---

## 2. Decision Register (`ADR-001` through `ADR-008`)

### `ADR-001`: Core Technology Stack Alignment with UCS553
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval
- **Context:** The project must implement a desktop Banking Management System aligned with **UCS553 — Enterprise Web Application** without external enterprise frameworks.
- **Alternatives:**
  1. Java 17 + JavaFX + Plain JDBC + SQLite + Maven.
  2. Spring Boot + Hibernate/JPA + React/web frontend.
  3. Swing + serialized flat files.
- **Decision:** Use **Java 17, JavaFX, Plain JDBC, SQLite, and Maven**, using standard JDK capabilities (Collections, Generics, Custom Exceptions, Java I/O, `java.util.logging`).
- **Rationale:** Directly covers syllabus concepts (JDBC transactions, DAO pattern, layered OOP, desktop GUI) with zero framework overhead.
- **Consequences & Outstanding Dependencies:** Requires manual dependency wiring in `bootstrap/` (`S`) and explicit SQLite JDBC boilerplate (`P`). Outstanding dependency: PR #1 approval + M2 `pom.xml` creation.
- **Related Documents:** [00_MASTER_PROJECT_CONTEXT.md](00_MASTER_PROJECT_CONTEXT.md), [TECHNICAL_CONTEXT.md](TECHNICAL_CONTEXT.md).

---

### `ADR-002`: Simplified Layered Architecture & Unified Customer Principal
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval (M0 drafted; simplified in M1)
- **Context:** Three teammates (`S`, `P`, `R`) need clear architectural boundaries without bloated wrapper layers or redundant entities.
- **Alternatives:**
  1. Strict unidirectional layered flow (`JavaFX UI -> Business Services -> DAO Interfaces -> JDBC -> SQLite`) with a unified `users` table and 1:1 `accounts` table in v1.
  2. Separate `customers` table + `CustomerDao` + `CustomerService` (M0 initial draft).
  3. Direct JDBC inside JavaFX controllers (rejected).
- **Decision:** Keep strict unidirectional layering (`ui` -> `service` -> `dao` -> `db`) and merge customer profile fields (`full_name`, `email`) into `users` (`role = 'CUSTOMER'`) with `1` account per customer in v1 (`UNIQUE(user_id)` on `accounts`).
- **Rationale:** Removes redundant joins and extra DAO/Service boilerplate while maintaining full normalization and service-layer security.
- **Consequences & Outstanding Dependencies:** Fixes P0 schema at 5 tables and P0 business services at 5 focused contracts (`AuthService`, `CustomerBankingService`, `TellerService`, `StaffService`, `AdminService`). Outstanding dependency: PR #1 team sign-off.
- **Related Documents:** [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md), [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md).

---

### `ADR-003`: Integer-Paise (`long`) Monetary Storage & `BigDecimal` Boundary Validation
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval (Finalized in M1)
- **Context:** Monetary calculations cannot tolerate floating-point drift (`float`/`double`), SQLite lacks a native `DECIMAL` type, and Java arithmetic must prevent overflow and excessive decimal precision.
- **Alternatives:**
  1. Persist and calculate internally as 64-bit integer **paise** (`long` in Java / `INTEGER` in SQLite), using `BigDecimal` inside `Money` for input parsing, scale validation, and display formatting.
  2. Store `BigDecimal` internally everywhere and persist `TEXT` strings in SQLite.
  3. Use `double` / `REAL` (rejected).
- **Decision:** Persist all monetary columns as **integer paise (`INTEGER`)**, compute internally using Java **`long`** with `Math.addExact`/`Math.subtractExact` (`INV-12`), and parse/validate boundary inputs using `BigDecimal` (`Money` rejects `<= 0`, `scale > 2`, and overflow — `INV-02`).
- **Rationale:** Combines exact SQLite `CHECK (balance_paise >= 0)` and integer `SUM(delta_paise)` reconciliation with accurate decimal boundary validation.
- **Consequences & Outstanding Dependencies:** `S` implements `Money`; `P` maps `long` columns; `R` renders formatted strings (`AccountSummary`, `TransactionView`) without `double`. Outstanding dependency: PR #1 team sign-off.
- **Related Documents:** [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

---

### `ADR-004`: `TransactionExecutor` Coordination, Audit Atomicity & Uncertain Commit Handling
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval (Finalized in M1 via Decisions B, D, E)
- **Context:** Multi-step financial mutations require one shared JDBC `Connection` (`INV-03`), sensitive admin/staff mutations require accurate audit logging, and commit-time I/O failures must not trigger unsafe automatic retries.
- **Alternatives:**
  1. Explicit `TransactionExecutor` + `TransactionWork<T>` + `BankingUnitOfWork` sharing one SQLite `Connection`, same-transaction `audit_events` for successful state mutations, `java.util.logging` for failures/denials, and `CommitOutcomeUncertainException` without automatic retries.
  2. ThreadLocal hidden `Connection` management or auto-retry loops on commit failure (rejected).
- **Decision:**
  - Use **`TransactionExecutor`**, **`TransactionWork<T>`**, and **`BankingUnitOfWork`** (`db/`) so `P` manages connection lifecycle, `PRAGMA foreign_keys = ON`, `autoCommit = false`, `commit()`, `rollback()`, and SQLite `busy_timeout`, while `S` owns business validation and journal orchestration.
  - **Audit Behavior (`Decision D`):** Write `audit_events` inside the same database transaction for sensitive state changes so rolled-back failures never leave false success audit rows; log rejected/failed attempts via `java.util.logging` without secrets or customer PII.
  - **Uncertain Commits (`Decision E`):** Distinguish pre-commit failures, confirmed commits, and uncertain commit outcomes (`CommitOutcomeUncertainException` carrying `referenceId` when available). Never automatically repeat ambiguous financial operations; direct the user to inspect history / `referenceId`.
  - **P0 vs. P1 Idempotency (`Decision B`):** Omit client `idempotencyKey` from P0 APIs while generating a unique server `reference_id` per committed operation; keep persistent client-request idempotency in P1.
- **Consequences & Outstanding Dependencies:** Keeps P0 simple and deterministic while acknowledging that a local desktop client cannot automatically disambiguate a mid-`commit()` I/O fault without P1 idempotency. Outstanding dependency: PR #1 team sign-off.
- **Related Documents:** [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

---

### `ADR-005`: Three-Role RBAC, Non-Teller Admin & Read-Only Oversight
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval (Finalized in M1 via Decision C)
- **Context:** Privileges across `CUSTOMER`, `STAFF`, and `ADMIN` must enforce clear operational boundaries at the service layer (`INV-05`, `INV-06`).
- **Alternatives:**
  1. Deny `ADMIN` all customer lookup methods vs. grant `ADMIN` read-only customer/account search & history inspection while strictly reserving cash teller actions to `STAFF`.
  2. Allow `ADMIN` automatic teller privileges (rejected).
- **Decision:** Enforce all role and ownership checks inside Business Services (`S`) using active `UserSession`. **`ADMIN`** has read-only customer/account lookup (`StaffService`) where needed for oversight (`Decision C`), **never** gains `TellerService` cash deposit/withdrawal permissions, and **`CUSTOMER`** accesses strictly their own single account (`userId` resolved from session).
- **Rationale:** Preserves separation of duties between branch cash operations (`STAFF`) and governance (`ADMIN`) without hindering administrative troubleshooting.
- **Consequences & Outstanding Dependencies:** `StaffService` accepts `STAFF` or `ADMIN` (read-only); `TellerService` accepts `STAFF` only; `CustomerBankingService` accepts `CUSTOMER` only. Outstanding dependency: PR #1 team sign-off.
- **Related Documents:** [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md), [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

---

### `ADR-006`: Equal Peer Module Ownership & Student-Proportionate Simplicity
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval
- **Context:** Three undergraduate students (`S`, `P`, `R`) collaborate on a shared repository under tight academic deadlines (`UCS553`).
- **Alternatives:**
  1. Defined peer ownership (`S`: Banking Engine/Security/Integration, `P`: Database/JDBC, `R`: JavaFX UI) with simple, readable Core Java conventions.
  2. Heavy enterprise abstractions (DI containers, factory hierarchies, event buses) — rejected.
- **Decision:** Maintain equal peer ownership across `S`, `P`, and `R` (no designated leader), allow `S` to support startup/navigation wiring during integration without removing `R`'s ownership of presentation, and enforce strict simplicity/comment rules ([../AGENTS.md](../AGENTS.md#5-student-level-engineering-code-quality--practical-collaboration)).
- **Rationale:** Protects delivery velocity, avoids overengineering, and keeps every module explainable by student contributors.
- **Consequences & Outstanding Dependencies:** Cross-module changes require PR review across affected owners. Outstanding dependency: PR #1 team sign-off.
- **Related Documents:** [../AGENTS.md](../AGENTS.md), [../docs/team_handoffs/01_S_BANKING_ENGINE.md](../docs/team_handoffs/01_S_BANKING_ENGINE.md), [02_P_DATABASE_AND_JDBC.md](../docs/team_handoffs/02_P_DATABASE_AND_JDBC.md), [03_R_JAVAFX_AND_UX.md](../docs/team_handoffs/03_R_JAVAFX_AND_UX.md).

---

### `ADR-007`: Balance-Plus-Transaction-Journal Model & Non-Destructive Reconciliation
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval (Revised from M0 single-table draft)
- **Context:** Transfers and deposits need atomic journal tracking (`INV-08`, `INV-09`), zero-sum transfer checks (`INV-04`), and P1 balance reconciliation (`INV-11`) without enterprise general-ledger complexity.
- **Alternatives:**
  1. Balance-plus-transaction-journal (`operations` + signed `transactions` entries + updated `accounts.balance_paise`).
  2. Single flat `transactions` table (M0 draft) or full bank-wide double-entry general ledger (rejected).
- **Decision:** Adopt the **balance-plus-transaction-journal** model (`operations` + `transactions`), writing 1 entry per affected account per operation (`+delta` deposit, `-delta` withdrawal, `-delta` and `+delta` summing to `0` for transfer), supporting non-destructive P1 reconciliation (`expected = SUM(delta_paise)`). Explicitly document that cash deposits/withdrawals are simulated external money movements, not a full general ledger.
- **Rationale:** Cleanly preserves running per-account snapshots (`balance_after_paise`), operation grouping (`reference_id`), and simple `SUM()` reconciliation.
- **Consequences & Outstanding Dependencies:** P0 `operations` and `transactions` DAOs are append/read-only. Outstanding dependency: PR #1 team sign-off (plus optional P2 verification after Gemini Deep Research review).
- **Related Documents:** [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md).

---

### `ADR-008`: First-Run Admin Bootstrap, Registration, `AC000001` Account Numbers & v1 Lifecycle
- **Status:** `RECOMMENDED — AWAITING APPROVAL`
- **Date:** Pending Team Approval (Finalized in M1 via Decisions A, F)
- **Context:** M0 left open initial administrator setup without default passwords, customer registration vs. account creation, synthetic account-number generation, and v1 account statuses.
- **Alternatives:**
  1. First-run local Admin bootstrap with user-supplied password + atomic setup lock; public Customer registration creating 1 `ACTIVE` account at `0` paise; deterministic `AC000001` account numbers derived from `accounts.id`; and two v1 statuses (`ACTIVE`, `FROZEN`).
  2. Hardcoded default admin credentials or random uncoordinated account strings (rejected).
- **Decision:**
  - **First-Run Admin (`Decision F`):** Available only when `COUNT(ADMIN) == 0`; uses user-supplied password hashed with salted `PBKDF2`; re-checks `COUNT(ADMIN) == 0` inside `TransactionExecutor` so concurrent/repeated attempts cannot create multiple bootstrap admins; no public `ADMIN` registration.
  - **Customer Registration & Account Numbers (`Decision A`):** Public registration hardcodes `role = CUSTOMER` and atomically inserts the `accounts` row (`balance_paise = 0`, `status = ACTIVE`), formatting `account_number` from assigned `accounts.id` (`AC000001` format) inside the same transaction. Account numbers are unique, never reused, stored with `UNIQUE`, and treated as public identifiers rather than secrets.
  - **v1 Account States:** `ACTIVE` and `FROZEN` only (account closure out of P0 scope); `FROZEN` permits customer viewing of own balance/history while rejecting deposits, withdrawals, incoming transfers, and outgoing transfers (`INV-07`).
- **Rationale:** Eliminates default-password risks, ensures atomic unique account numbers without collision loops, and keeps v1 state rules concise.
- **Consequences & Outstanding Dependencies:** `AccountDao.createAccount` handles insert + ID retrieval + `AC%06d` update atomically. Outstanding dependency: PR #1 team sign-off.
- **Related Documents:** [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md), [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md).
