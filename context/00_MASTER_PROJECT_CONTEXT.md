# Master Project Context (`00_MASTER_PROJECT_CONTEXT.md`)

> **Authority Level:** Stable Master Reference for Project Identity, Scope, and Boundaries.
> **Current Phase:** **Implemented & Verified Working Baseline (`P0` + `P1` Complete; `P2` Deferred; Ready for Teammate Onboarding)**

---

## 1. Project Identity

| Attribute | Detail |
| :--- | :--- |
| **Project Title** | Banking Management System |
| **Course** | UCS553 — Enterprise Web Application |
| **Institution** | Thapar Institute of Engineering and Technology (TIET), Patiala |
| **Application Type** | Core Java 17 Desktop Application (JavaFX + Plain JDBC + SQLite + Maven) |
| **Repository Role** | Shared collaborative workspace for engineering, documentation, and academic reporting |
| **Domain Classification** | Educational Banking Simulation (Fictitious Customers & Simulated Funds Only) |

---

## 2. Problem Statement & Academic Objective

The goal of this project is to build the **simplest correct, explainable, and well-verified Core Java banking simulation** aligned with the **UCS553** syllabus—without overengineering or relying on Spring Boot/Hibernate:

- Clean layered separation (`JavaFX UI -> Business Services -> DAO Interfaces -> JDBC Implementations -> SQLite`).
- Integer-paise monetary representation (`long` / `INTEGER`) paired with `BigDecimal` input validation (`ADR-003`).
- Explicit atomic balance-plus-transaction-journal coordination via `TransactionExecutor` (`ADR-004`, `ADR-007`).
- Service-layer RBAC and customer ownership enforcement across three distinct roles (`ADR-005`).

---

## 3. Confirmed Requirements vs. Proposed M1 Specifications

### Confirmed Core Baseline
- Three-role Java 17 desktop application: **Customer**, **Bank Staff**, and **Administrator**.
- Local SQLite persistence via **Plain JDBC** and the **DAO pattern** (no ORM).
- Fictitious customer data and simulated money only; not a licensed or production-ready banking system.
- Equal peer module ownership across **S**, **P**, and **R**, keeping code student-maintainable ([../AGENTS.md](../AGENTS.md#5-student-level-engineering-code-quality--practical-collaboration)).

### Working Baseline (`APPROVED BY S — IMPLEMENTED & VERIFIED`)
- **P0 Core Application (`IMPLEMENTED`):** First-run Admin bootstrap (user-supplied password), public Customer registration (1 `ACTIVE` account starting at `0` paise), Admin staff provisioning, Staff search + cash deposit/withdrawal, Customer balance/history/transfer, SQLite persistence via `TransactionExecutor`/`BankingUnitOfWork` and Plain JDBC DAOs.
- **P1 Differentiators (`IMPLEMENTED`):** 7-table schema (`users`, `accounts`, `operations`, `transactions`, `audit_events`, `account_action_requests`, `idempotency_records`), non-destructive balance reconciliation, isolated rollback sandbox, administrative audit viewer, maker-checker account freeze/unfreeze, formula-safe CSV statement export, and persistent idempotency protection (`idempotency_records`).
- **P2 Optional Extensions (`DEFERRED`):** Explainable fraud alerts, transaction disputes, multiple account types, and research-justified controls remain deferred.

---

## 4. Intended Users and Three-Role Model (M1 Policy)

1. **Customer** — Publicly registers a `CUSTOMER` identity with 1 zero-balance account in v1; views own account balance and transaction history; transfers simulated funds to another active customer account; exports CSV statements (P1).
2. **Bank Staff** — Logs in with `ADMIN`-provisioned credentials; searches customers and accounts; executes simulated cash deposits and withdrawals; requests account freeze/unfreeze (P1).
3. **Administrator** — Performs first-run secure setup; creates and deactivates `STAFF` accounts; views administrative statistics and audit events; reviews freeze/unfreeze requests (P1). Administrators do **not** automatically act as tellers.

See [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md).

---

## 5. Proposed Technology Baseline & UCS553 Curriculum Alignment

| Technology / Concept | Role in System | Status |
| :--- | :--- | :---: |
| **Java 17** | Core OOP implementation language | `IMPLEMENTED & VERIFIED` |
| **JavaFX 17** | Desktop GUI presentation layer | `IMPLEMENTED & VERIFIED` |
| **Plain JDBC + SQLite** | 7-table relational persistence, `PreparedStatement`, atomic transactions | `IMPLEMENTED & VERIFIED` |
| **DAO Design Pattern** | Separates SQL queries from business services | `IMPLEMENTED & VERIFIED` |
| **Collections, Generics & Custom Exceptions** | DTO lists, `TransactionWork<T>`, typed domain/auth exceptions | `IMPLEMENTED & VERIFIED` |
| **Java I/O & `java.util.logging`** | P1 CSV statement export, config loading & operational diagnostics | `IMPLEMENTED & VERIFIED` |
| **Maven & Maven Wrapper** | Build, test & JavaFX launch lifecycle (`pom.xml`, `mvnw`, `mvnw.cmd`) | `IMPLEMENTED & VERIFIED` |

**Explicit Exclusions:** Spring Boot, Hibernate/JPA, React, microservices, external payment rails, and real identity/KYC APIs.

---

## 6. High-Level Architecture Overview (M1 v2.0 Proposal)

1. **JavaFX Presentation (`com.tiet.banking.ui.*` — R):** Screens, forms, `TableView` displays, CSS styling; interacts solely with Business Services via DTOs.
2. **Business Services & Security (`com.tiet.banking.service.*`, `security.*`, `exception.*` — S):** Enforces `INV-01`–`INV-12`, role/ownership checks, `Money` validation, and `TransactionExecutor` orchestration.
3. **DAO & SQLite Persistence (`com.tiet.banking.dao.*`, `dao.jdbc.*`, `db.*` — P):** Implements `TransactionExecutor`, `BankingUnitOfWork`, and parameterized SQLite DAOs with `PRAGMA foreign_keys = ON`.
4. **Shared Models, DTOs & Bootstrap:** `model.*`, `dto.*`, `util.*`, `bootstrap.*`.

See [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md).

---

## 7. Team Members & Peer Responsibilities

Three equal peers (**S**, **P**, **R** — no designated team leader):
- **S:** Banking Engine, Security, Business Services, Integration & Service/Integration Tests ([01_S_BANKING_ENGINE.md](../docs/team_handoffs/01_S_BANKING_ENGINE.md))
- **P:** SQLite Database, Plain JDBC, DAOs, `TransactionExecutor` & Persistence Tests ([02_P_DATABASE_AND_JDBC.md](../docs/team_handoffs/02_P_DATABASE_AND_JDBC.md))
- **R:** JavaFX Presentation, Visual Design, Workflows & UI Documentation ([03_R_JAVAFX_AND_UX.md](../docs/team_handoffs/03_R_JAVAFX_AND_UX.md))

---

## 8. Confirmed Scope Boundaries & Pending Review Gates

- **In Scope:** Single-node local desktop simulation, fictitious customer records, 1 account per customer in v1, `ACTIVE`/`FROZEN` states, atomic balance-plus-journal ledger, and reproducible tests (`TC-01`–`TC-17`).
- **Out of Scope:** Account closure in P0, multi-account per customer in v1, full bank-wide general ledger, automatic financial retries in P0, real RBI/KYC integrations.
- **Remaining Review / Open Gates:**
  1. Working development baseline approved by **S** (`ADR-001`–`ADR-008` implemented in `src/`); shared with **P** and **R** for teammate onboarding and review.
  2. External **Gemini Deep Research** review remains pending (`docs/research/RESEARCH_FINDINGS.md`, `REGULATORY_TRACEABILITY.md`).

---

## 9. Authoritative Document Map

- **Agent & Coding Rules:** [../AGENTS.md](../AGENTS.md)
- **Project Vision & Technical Context:** [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md), [TECHNICAL_CONTEXT.md](TECHNICAL_CONTEXT.md)
- **Status & Implementation Tracking:** [CURRENT_STATUS.md](CURRENT_STATUS.md), [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)
- **Decisions & Risks:** [DECISIONS.md](DECISIONS.md), [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
- **Session Continuity:** [SESSION_LOG.md](SESSION_LOG.md), [SESSION_HANDOFF.md](SESSION_HANDOFF.md)
- **M1 Architecture Specs:** [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md), [DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md), [ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md), [TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md)
- **Requirements, Testing & Research:** [../docs/requirements/REQUIREMENTS.md](../docs/requirements/REQUIREMENTS.md), [FEATURE_MATRIX.md](../docs/requirements/FEATURE_MATRIX.md), [../docs/testing/TEST_PLAN.md](../docs/testing/TEST_PLAN.md), [../docs/research/RESEARCH_FINDINGS.md](../docs/research/RESEARCH_FINDINGS.md), [REGULATORY_TRACEABILITY.md](../docs/research/REGULATORY_TRACEABILITY.md)

---

## 10. Academic & Security Limitations Disclaimer

1. **Educational Simulation Only:** Processes fictitious identities and simulated money for **UCS553** coursework evaluation only.
2. **No Regulatory Compliance Claims:** Pending Gemini Deep Research review; makes no claim of RBI approval, statutory compliance, or production banking security.
3. **Desktop SQLite Threat Boundary:** A local SQLite file cannot protect records against an OS user with direct filesystem read/write access (`RISK-03`).
