# Banking Management System (UCS553 — Enterprise Web Application)

> **Project Status:** **M1 — Architecture v2.0 & Contract Specification (Awaiting Team Review; Implementation Not Started)**
> No Java source code, SQL schema, `pom.xml`, JavaFX screens, or automated tests have been implemented yet. All M1 architectural contracts, database specifications, and decisions remain **provisional proposals** pending team approval and external research review.

---

## 1. Academic Context & Overview

- **Course:** UCS553 — Enterprise Web Application
- **Institution:** Thapar Institute of Engineering and Technology, Patiala
- **Project Type:** Core Java 17 Desktop Application with JavaFX, Plain JDBC, and SQLite
- **Team:** Three equal peer members — **S**, **P**, and **R**

The **Banking Management System** is an educational desktop simulation using fictitious customers and simulated funds. It demonstrates clean Core Java engineering: layered separation of concerns, exact integer-paise monetary arithmetic, an atomic balance-plus-transaction-journal model over plain JDBC/SQLite, service-layer RBAC, custom exceptions, and focused JavaFX workflows.

---

## 2. Three User Roles (M1 Policy)

1. **Customer** — Publicly registers a `CUSTOMER` account (creating one `ACTIVE` bank account at zero balance in v1), views own account balance and transaction history, transfers simulated funds to another customer's active account, and exports CSV statements (P1).
2. **Bank Staff** — Logs in with administrator-provisioned credentials, searches customers and accounts, executes simulated cash deposits and withdrawals, reviews authorized customer account details, and submits freeze/unfreeze requests (P1).
3. **Administrator** — Completes secure first-run administrator setup, provisions and deactivates staff accounts, views administrative statistics and audit events, and reviews account freeze/unfreeze requests (P1). Administrators do **not** automatically act as cash tellers.

See [docs/architecture/ROLE_PERMISSIONS.md](docs/architecture/ROLE_PERMISSIONS.md) for the complete role-permission matrix.

---

## 3. Proposed Features (Prioritized)

| Tier | Description | Proposed Capabilities |
| :--- | :--- | :--- |
| **P0 — Essential** | Working Core Banking Application | First-run administrator setup, customer registration & zero-balance account creation, login/logout, three-role authorization, admin staff provisioning, staff customer/account search, staff simulated cash deposit & withdrawal, customer balance enquiry, customer transfers, transaction history, SQLite persistence, atomic JDBC operations, JavaFX dashboards, validation & custom exceptions, automated service/DB tests |
| **P1 — Differentiators** | Governance, Audit & Integrity Controls | Balance reconciliation (non-destructive), administrative audit viewer, maker-checker account freeze/unfreeze requests, CSV statement export, persistent idempotency protection |
| **P2 — Optional (Research-Dependent)** | Future Simulation Extensions | Explainable fraud alerts, transaction dispute workflow, multiple account types, research-justified extensions |

Full details are tracked in [docs/requirements/REQUIREMENTS.md](docs/requirements/REQUIREMENTS.md) and [docs/requirements/FEATURE_MATRIX.md](docs/requirements/FEATURE_MATRIX.md).

---

## 4. Proposed Technology Baseline

Aligned with the UCS553 curriculum (awaiting formal approval in [context/DECISIONS.md](context/DECISIONS.md)):

- **Language:** Java 17
- **UI Framework:** JavaFX
- **Persistence:** SQLite via Plain JDBC (`java.sql.*`, `PreparedStatement`, explicit `TransactionExecutor` transactions)
- **Build & Packaging:** Maven (deferred to M2)
- **Core Java Practices:** Layered Architecture, DAO Pattern, DTOs, Collections & Generics, Custom Exceptions, Java I/O, `java.util.logging`

---

## 5. High-Level Layered Architecture (Proposed M1 v2.0)

```text
+-------------------------------------------------------------------+
|  Presentation Layer (JavaFX UI — Owner: R)                        |
|  com.tiet.banking.ui.{common, auth, customer, staff, admin}       |
+-------------------------------------------------------------------+
                                  |  uses DTOs & Service contracts
                                  v
+-------------------------------------------------------------------+
|  Business Service, Auth & Validation Layer (Owner: S)             |
|  com.tiet.banking.{service, security, dto, exception, model}      |
+-------------------------------------------------------------------+
                                  |  uses DAO & TransactionExecutor
                                  v
+-------------------------------------------------------------------+
|  Persistence Layer (Plain JDBC & SQLite — Owner: P)               |
|  com.tiet.banking.{dao, dao.jdbc, db} -> data/*.db (gitignored)   |
+-------------------------------------------------------------------+
```

Cross-cutting components include `bootstrap/` (wiring/startup) and `util/` (validation, Java I/O, logging, configuration). See [docs/architecture/SYSTEM_ARCHITECTURE.md](docs/architecture/SYSTEM_ARCHITECTURE.md).

---

## 6. Repository Structure

```text
Banking_Management_System_JAVA/
├── README.md
├── AGENTS.md
├── .gitignore
├── context/                          # Persistent engineering context & session state
│   ├── 00_MASTER_PROJECT_CONTEXT.md
│   ├── PROJECT_CONTEXT.md
│   ├── TECHNICAL_CONTEXT.md
│   ├── CURRENT_STATUS.md
│   ├── IMPLEMENTATION_STATUS.md
│   ├── KNOWN_ISSUES.md
│   ├── DECISIONS.md
│   ├── SESSION_LOG.md
│   └── SESSION_HANDOFF.md
├── docs/                             # M1 architecture specs, requirements, research, testing & handoffs
│   ├── architecture/
│   │   └── diagrams/
│   ├── requirements/
│   ├── research/
│   ├── testing/
│   ├── report/
│   │   └── screenshots/
│   └── team_handoffs/
├── src/                              # Placeholder package tree (.gitkeep only in M0/M1)
│   ├── main/java/com/tiet/banking/
│   ├── main/resources/
│   └── test/java/com/tiet/banking/
└── data/                             # Runtime SQLite storage directory (.gitkeep only; DB files ignored)
```

---

## 7. Team Responsibilities (Equal Peer Ownership)

| Member | Module Scope | Primary Owned Directories | Detailed Handoff |
| :---: | :--- | :--- | :--- |
| **S** | Banking Engine, Security, Services & Integration | `service/`, `security/`, `exception/`, `bootstrap/`, `util/`, shared `model/` & `dto/`, `test/.../service/`, `test/.../integration/` | [01_S_BANKING_ENGINE.md](docs/team_handoffs/01_S_BANKING_ENGINE.md) |
| **P** | Database, JDBC, Persistence & Reliability | `db/`, `dao/`, `dao/jdbc/`, `resources/db/`, `data/`, shared `model/`, `test/.../dao/` | [02_P_DATABASE_AND_JDBC.md](docs/team_handoffs/02_P_DATABASE_AND_JDBC.md) |
| **R** | JavaFX Presentation & User Experience | `ui/common/`, `ui/auth/`, `ui/customer/`, `ui/staff/`, `ui/admin/`, `resources/styles/`, `docs/report/screenshots/`, shared `dto/` | [03_R_JAVAFX_AND_UX.md](docs/team_handoffs/03_R_JAVAFX_AND_UX.md) |

---

## 8. Current Development Status

- **Completed Milestone (`M0`):** Initial repository structure and persistent context foundation committed on `main`.
- **Current Milestone (`M1`):** Architecture v2.0 and contract specification under review on branch `docs/m1-architecture-contracts`.
- **Application Code:** **Not Started** (`src/` and `data/` contain `.gitkeep` placeholders only).
- **Pending Before M2 Implementation:**
  1. Teammate review and approval of `ADR-001` through `ADR-008` in [context/DECISIONS.md](context/DECISIONS.md) and M1 contracts under `docs/architecture/`.
  2. External Gemini Deep Research review to populate [docs/research/RESEARCH_FINDINGS.md](docs/research/RESEARCH_FINDINGS.md) and [docs/research/REGULATORY_TRACEABILITY.md](docs/research/REGULATORY_TRACEABILITY.md).

See [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md) and [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md).

---

## 9. Documentation Navigation

| Category | Document | Purpose |
| :--- | :--- | :--- |
| **AI & Code Rules** | [AGENTS.md](AGENTS.md) | Mandatory engineering rules, code simplicity policy, authority hierarchy & Git conventions |
| **Context** | [context/00_MASTER_PROJECT_CONTEXT.md](context/00_MASTER_PROJECT_CONTEXT.md) | Stable master reference for the entire project |
| **Context** | [context/PROJECT_CONTEXT.md](context/PROJECT_CONTEXT.md) | Product vision, roles, workflows, milestones & academic report link |
| **Context** | [context/TECHNICAL_CONTEXT.md](context/TECHNICAL_CONTEXT.md) | Technical stack, paise arithmetic, `TransactionExecutor`, invariants & logging |
| **Context** | [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md) | Truthful snapshot of project state |
| **Context** | [context/IMPLEMENTATION_STATUS.md](context/IMPLEMENTATION_STATUS.md) | Module-by-module implementation tracking table (`NOT STARTED`) |
| **Context** | [context/KNOWN_ISSUES.md](context/KNOWN_ISSUES.md) | Design risks and open questions |
| **Context** | [context/DECISIONS.md](context/DECISIONS.md) | Architecture Decision Records (`ADR-001`–`ADR-008`) |
| **Context** | [context/SESSION_LOG.md](context/SESSION_LOG.md) | Chronological development log |
| **Context** | [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md) | Continuation instructions for the next session |
| **Architecture** | [docs/architecture/SYSTEM_ARCHITECTURE.md](docs/architecture/SYSTEM_ARCHITECTURE.md) | Layered architecture, v2.0 simplifications & `INV-01`–`INV-12` invariants |
| **Architecture** | [docs/architecture/DATABASE_DESIGN.md](docs/architecture/DATABASE_DESIGN.md) | 5-table relational specification, constraints, indexes & correctness questions |
| **Architecture** | [docs/architecture/SERVICE_CONTRACTS.md](docs/architecture/SERVICE_CONTRACTS.md) | Domain models, DTOs, service interfaces & DAO/UoW contracts |
| **Architecture** | [docs/architecture/ROLE_PERMISSIONS.md](docs/architecture/ROLE_PERMISSIONS.md) | Role matrix, first-run admin bootstrap, registration & account state policies |
| **Architecture** | [docs/architecture/TRANSACTION_MODEL.md](docs/architecture/TRANSACTION_MODEL.md) | Monetary model, balance-plus-journal ledger, `TransactionExecutor` & SQLite rules |
| **Requirements** | [docs/requirements/REQUIREMENTS.md](docs/requirements/REQUIREMENTS.md) | Functional, non-functional & UCS553 curriculum alignment |
| **Requirements** | [docs/requirements/FEATURE_MATRIX.md](docs/requirements/FEATURE_MATRIX.md) | P0 / P1 / P2 prioritized feature matrix |
| **Research** | [docs/research/RESEARCH_FINDINGS.md](docs/research/RESEARCH_FINDINGS.md) | Placeholder for pending external Gemini Deep Research synthesis |
| **Research** | [docs/research/REGULATORY_TRACEABILITY.md](docs/research/REGULATORY_TRACEABILITY.md) | Placeholder table for Indian banking/RBI conceptual traceability |
| **Testing** | [docs/testing/TEST_PLAN.md](docs/testing/TEST_PLAN.md) | Proposed unit, DAO, integration & UI test plan (`NOT EXECUTED`) |
| **Testing** | [docs/testing/DEMO_SCENARIOS.md](docs/testing/DEMO_SCENARIOS.md) | Planned academic evaluation walkthrough scenarios |

---

## 10. Contribution Workflow

1. Never commit directly to `main`; create focused branches (e.g., `docs/m1-architecture-contracts`, `feature/project-foundation`, `feature/banking-engine`, `feature/database-jdbc`, `feature/javafx-ui`).
2. Review [AGENTS.md](AGENTS.md) and your module handoff before editing code or documentation.
3. Update shared contracts (`docs/architecture/*`) and [context/DECISIONS.md](context/DECISIONS.md) before changing cross-module boundaries.
4. Open a Pull Request for teammate cross-review with reproducible verification notes and updated context files.

---

## 11. Academic Simulation Disclaimer

This software project is strictly an **educational university simulation** created for coursework (**UCS553 — Enterprise Web Application**, Thapar Institute of Engineering and Technology). All customer identities, accounts, balances, and transactions use **fictitious data and simulated funds**. It is not a licensed or production-ready banking system and makes no claim of RBI approval or regulatory compliance.
