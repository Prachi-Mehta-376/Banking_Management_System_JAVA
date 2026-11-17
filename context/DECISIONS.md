# Architecture Decision Records (`DECISIONS.md`)

> **Governance Rule:** No decision is `ACCEPTED` until formally reviewed and approved by teammates **S**, **P**, and **R**. Approval dates must never be fabricated. In M1, proposals are labeled `PROPOSED — AWAITING TEAM REVIEW` or `PROVISIONAL — AWAITING TEAM REVIEW`.

---

## 1. Decision Record Template (For Future Entries)

```markdown
### ADR-XXX: <Concise Decision Title>
- **Status:** PROPOSED — AWAITING TEAM REVIEW | PROVISIONAL — AWAITING TEAM REVIEW | ACCEPTED | SUPERSEDED | DEPRECATED
- **Date:** Pending Team Approval (record YYYY-MM-DD when accepted)
- **Context:** What architectural problem or constraint requires a choice?
- **Alternatives:** Viable options considered.
- **Decision:** The chosen approach (including M1 revisions if any).
- **Rationale:** Why this choice fits UCS553 goals, reliability, and maintainability.
- **Consequences & Integration Impact:** Trade-offs, constraints, and cross-module effects.
- **Related Documents:** Relative links to affected files.
```

---

## 2. Decision Register (`ADR-001` through `ADR-008`)

### `ADR-001`: Core Technology Stack Alignment with UCS553
- **Status:** `PROVISIONAL — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (M0 drafted; retained unchanged in M1)
- **Context:** The project must implement a desktop Banking Management System aligned with **UCS553 — Enterprise Web Application** without external enterprise frameworks.
- **Alternatives:**
  1. Java 17 + JavaFX + Plain JDBC + SQLite + Maven.
  2. Spring Boot + Hibernate/JPA + React/web frontend.
  3. Swing + serialized flat files (no relational DB).
- **Decision:** Use **Java 17, JavaFX, Plain JDBC, SQLite, and Maven**, using standard JDK capabilities (Collections, Generics, Custom Exceptions, Java I/O, `java.util.logging`).
- **Rationale:** Directly covers syllabus concepts (JDBC transactions, DAO pattern, layered OOP, desktop GUI) with zero server/container complexity.
- **Consequences & Integration Impact:** Requires manual dependency assembly in `bootstrap/` (`S`) and explicit SQLite JDBC boilerplate (`P`), avoiding heavy frameworks.
- **Related Documents:** [00_MASTER_PROJECT_CONTEXT.md](00_MASTER_PROJECT_CONTEXT.md), [TECHNICAL_CONTEXT.md](TECHNICAL_CONTEXT.md).

---

### `ADR-002`: Simplified Layered Architecture & Unified Customer Principal
- **Status:** `PROVISIONAL — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (**Revised in M1** from M0 6-table draft)
- **Context:** Three teammates (`S`, `P`, `R`) need clear architectural boundaries without bloated wrapper layers or redundant entity duplication.
- **Alternatives:**
  1. Strict unidirectional layered flow (`JavaFX UI -> Business Services -> DAO Interfaces -> JDBC -> SQLite`) with a unified `users` table and 1:1 `accounts` table in v1.
  2. Separate `customers` table + `CustomerDao` + `CustomerService` (M0 initial draft).
  3. Direct JDBC inside JavaFX controllers (rejected — violates separation of concerns).
- **Decision:** Keep strict unidirectional layering (`ui` -> `service` -> `dao` -> `db`) and **simplify M0 by merging customer identity into `users` (`role = 'CUSTOMER'`)** rather than maintaining a separate `customers` table in v1.
- **Rationale:** In v1, each customer has at most one bank account (`UNIQUE(user_id)` on `accounts`). Merging customer profile fields (`full_name`, `email`) into `users` removes redundant joins and a redundant DAO/Service without sacrificing normalization or security.
- **Consequences & Integration Impact:** Simplifies `P`'s schema from 6 to 5 core P0 tables and focuses `S`'s services on `AuthService`, `CustomerBankingService`, `TellerService`, `StaffService`, and `AdminService`. UI still consumes `dto.*` exclusively.
- **Related Documents:** [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md), [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md).

---

### `ADR-003`: Integer-Paise (`long`) Monetary Storage & `BigDecimal` Boundary Validation
- **Status:** `PROPOSED — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (**Refined in M1** from M0 `BigDecimal` + minor-unit options)
- **Context:** Monetary calculations cannot tolerate binary floating-point drift (`float`/`double`), and SQLite has no native fixed-point `DECIMAL` type. Meanwhile, Java domain arithmetic must prevent overflow and scale errors.
- **Alternatives:**
  1. Persist and calculate internally as 64-bit integer **paise** (`long` in Java / `INTEGER` in SQLite), using `BigDecimal` exclusively to parse, validate (`<= 2` decimals, `> 0`), and format user-facing values inside a `Money` value class.
  2. Store `BigDecimal` internally everywhere and persist `TEXT` strings in SQLite.
  3. Use `double` / `REAL` (rejected).
- **Decision:** Persist all monetary columns as **integer paise (`INTEGER`)**, use Java **`long`** internally with `Math.addExact`/`Math.subtractExact` overflow checks (`INV-12`), and use **`BigDecimal`** inside `Money` for boundary parsing and validation (rejecting negative, zero, `> 2` decimal places, and `long` overflow — `INV-02`).
- **Rationale:** Combines exact integer SQLite arithmetic (`CHECK (balance_paise >= 0)`, exact `SUM(delta_paise)` reconciliation) with clean decimal parsing/formatting at the Java boundary.
- **Consequences & Integration Impact:** `S` owns `Money` parsing/validation; `P` maps `ResultSet.getLong(...)` directly to paise fields; `R` displays formatted currency strings (`AccountSummary`, `TransactionView`) without converting to `double`.
- **Related Documents:** [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

---

### `ADR-004`: `TransactionExecutor`, `TransactionWork<T>` & `BankingUnitOfWork` Coordination
- **Status:** `PROPOSED — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (**Refined in M1** from M0 open `RISK-01`)
- **Context:** Multi-step financial mutations (e.g., transfer debit + credit + `operations` + `transactions`) require a single shared JDBC `Connection` so everything commits or rolls back atomically (`INV-03`), without leaking raw `java.sql.Connection` into services (`S`).
- **Alternatives:**
  1. Explicit `TransactionExecutor.execute(TransactionWork<T>)` passing a `BankingUnitOfWork` that exposes DAOs bound to one shared SQLite `Connection`.
  2. ThreadLocal hidden `Connection` management inside every DAO.
  3. Passing raw `java.sql.Connection` across service signatures (rejected — couples business services to JDBC API).
- **Decision:** Adopt **`TransactionExecutor`**, **`TransactionWork<T>`**, and **`BankingUnitOfWork`** (`com.tiet.banking.db`) as the explicit atomic boundary between `S` (orchestration, validation, invariants) and `P` (connection lifecycle, `PRAGMA foreign_keys = ON`, `commit`, `rollback`, resource cleanup, SQLite lock handling).
- **Rationale:** Makes transactional boundaries explicit, easily testable with failure-injection stubs (`TC-10`, `TC-11`), and immediately understandable to undergraduate developers without ThreadLocal magic.
- **Consequences & Integration Impact:** `P` implements `TransactionExecutor` and connection-bound DAOs; `S` invokes `TransactionExecutor` for every state-changing multi-step operation and never retries uncertain commits automatically in P0.
- **Related Documents:** [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

---

### `ADR-005`: Three-Role RBAC, Non-Teller Admin & Service-Layer Ownership Enforcement
- **Status:** `PROPOSED — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (**Refined in M1**)
- **Context:** The system has three operational roles (`CUSTOMER`, `STAFF`, `ADMIN`) with strict privilege boundaries (`INV-05`, `INV-06`).
- **Alternatives:**
  1. UI-only screen hiding.
  2. Mandatory service-layer authorization against active `UserSession`, separating `ADMIN` governance from `STAFF` cash-teller permissions and enforcing customer ownership by session `userId`.
- **Decision:** Enforce all role checks and single-account customer ownership strictly inside Business Services (`S`). **Administrators do not automatically receive teller permissions** (`depositCash`/`withdrawCash` require `STAFF`). **Customers cannot grant themselves `STAFF`/`ADMIN` roles** and cannot query or debit another customer's account.
- **Rationale:** Prevents privilege escalation and keeps separation of duties explicit and testable (`TC-04`) independently of JavaFX.
- **Consequences & Integration Impact:** Services depend on `SecurityContext` (`UserSession`); UI role routing reflects these rules without acting as the security gate.
- **Related Documents:** [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md), [../AGENTS.md](../AGENTS.md).

---

### `ADR-006`: Equal Peer Module Ownership & Student-Proportionate Simplicity
- **Status:** `PROVISIONAL — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (**Updated in M1** with student-engineering guidelines)
- **Context:** Three undergraduate students (`S`, `P`, `R`) collaborate on a shared repository under academic time constraints (`UCS553`).
- **Alternatives:**
  1. Ad-hoc shared editing across all layers (high merge conflict risk).
  2. Defined primary peer ownership (`S`: Banking Engine/Security/Integration, `P`: Database/JDBC, `R`: JavaFX UI) paired with minimal, readable Core Java abstractions.
- **Decision:** Maintain equal peer ownership across `S`, `P`, and `R` (no designated leader), allow `S` to support startup/navigation wiring during integration without diminishing `R`'s ownership of presentation, and enforce strict simplicity/comment hygiene (`AGENTS.md` Section 5).
- **Rationale:** Enables parallel M2 implementation on focused feature branches while keeping every module readable and explainable by students during viva evaluation.
- **Consequences & Integration Impact:** Cross-module changes (`model/`, `dto/`, `service/`, `dao/`) require PR review across affected owners.
- **Related Documents:** [../AGENTS.md](../AGENTS.md), [../docs/team_handoffs/01_S_BANKING_ENGINE.md](../docs/team_handoffs/01_S_BANKING_ENGINE.md), [02_P_DATABASE_AND_JDBC.md](../docs/team_handoffs/02_P_DATABASE_AND_JDBC.md), [03_R_JAVAFX_AND_UX.md](../docs/team_handoffs/03_R_JAVAFX_AND_UX.md).

---

### `ADR-007`: Balance-Plus-Transaction-Journal Model & Non-Destructive Reconciliation
- **Status:** `PROPOSED — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (**Revised in M1** from M0 single-table `transactions` idea)
- **Context:** A single flat `transactions` table makes it awkward to model multi-account transfers cleanly alongside cash operations and verify balance conservation (`INV-04`, `INV-08`).
- **Alternatives:**
  1. Single flat `transactions` table with nullable `source_account_id` and `destination_account_id` (M0 initial concept).
  2. **Balance-plus-transaction-journal** (`operations` parent + signed `transactions` account entries + updated `accounts.balance_paise`).
  3. Full bank-wide double-entry general ledger with vault/cash contra accounts (rejected — overengineered for v1 simulation).
- **Decision:** Adopt the **balance-plus-transaction-journal** model (`operations` + `transactions`), writing 1 entry per affected account per operation (`+delta` for deposit, `-delta` for withdrawal, `-delta` and `+delta` summing to `0` for transfer), paired with non-destructive P1 reconciliation (`expected = SUM(delta_paise)`, reporting mismatches without altering stored balances — `INV-11`). Explicitly state this is **not** a full bank-wide double-entry general ledger.
- **Rationale:** Cleanly preserves operation-level grouping, per-account running balances (`balance_after_paise`), zero-sum transfer checks, and simple SQL `SUM()` reconciliation.
- **Consequences & Integration Impact:** `P` implements `OperationDao` + `TransactionDao` (`insert` & query only — append-only `INV-09`); `S` computes signed deltas and `balance_after_paise` atomically inside `TransactionExecutor`.
- **Related Documents:** [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md).

---

### `ADR-008`: First-Run Admin Bootstrap, Public Customer Registration & v1 Account Lifecycle
- **Status:** `PROPOSED — AWAITING TEAM REVIEW`
- **Date:** Pending Team Approval (**Added in M1**)
- **Context:** Initial M0 documents left open how administrators first appear without hardcoded default passwords (`admin123`), whether customers register themselves or only via Staff, and which account statuses exist in v1 (`RISK-04`, `RISK-05`).
- **Alternatives:**
  1. Ship hardcoded default admin credentials in source/seed SQL (rejected — insecure anti-pattern).
  2. First-run conditional Admin setup (user-supplied password, permanently locked once `COUNT(ADMIN) > 0`), public Customer registration creating 1 `ACTIVE` account at `0` paise, and two v1 account states (`ACTIVE`, `FROZEN`).
- **Decision:**
  - **Admin Bootstrap:** Startup checks `isInitialAdminSetupRequired()`; if zero admins exist, operator creates initial `ADMIN` with user-supplied password (recorded in `audit_events`), after which bootstrap permanently locks (`ConflictException`).
  - **Public Customer Registration:** Publicly creates `CUSTOMER` user + 1 `ACTIVE` account at `balance_paise = 0` atomically; `STAFF` and `ADMIN` cannot be self-registered.
  - **Account States in v1:** Support `ACTIVE` and `FROZEN` only (account closure out of P0 scope). `FROZEN` accounts allow read-only view/history while rejecting deposits, withdrawals, incoming transfers, and outgoing transfers (`INV-07`).
- **Rationale:** Eliminates default-password vulnerabilities, solves initial liquidity cleanly (new accounts start at `₹0.00`, funded only via Staff cash deposit), and minimizes state-machine complexity for v1.
- **Consequences & Integration Impact:** Adds `setupInitialAdmin` and `registerCustomer` to `AuthService`, `UNIQUE(user_id)` on `accounts`, and `CHECK (status IN ('ACTIVE', 'FROZEN'))` in SQLite.
- **Related Documents:** [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md), [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md).
