# Banking Management System (UCS553 — Enterprise Web Application)

> **Project Status:** **M0 — Documentation & Architectural Scaffolding Complete (Implementation Not Started)**
> No Java source code, SQL schema, `pom.xml`, JavaFX screens, or automated tests have been implemented yet. All architectural contracts, database designs, and feature priorities documented here are **provisional proposals** pending team approval and research review.

---

## 1. Academic Context & Overview

- **Course:** UCS553 — Enterprise Web Application
- **Institution:** Thapar Institute of Engineering and Technology, Patiala
- **Project Type:** Core Java 17 Desktop Application with JavaFX, Plain JDBC, and SQLite
- **Team:** Three equal peer members — **S**, **P**, and **R**

The **Banking Management System** is an educational desktop simulation designed to demonstrate enterprise software engineering principles in Core Java: clean layered architecture, role-based access control, reliable database transaction coordination via plain JDBC, explicit domain exception handling, auditability, and structured JavaFX presentation.

---

## 2. Three User Roles

The proposed application supports three distinct operational roles:

1. **Customer** — Views own profile and owned account balances, views transaction history, exports simulated statements, and initiates transfers between accounts (subject to ownership and balance validation).
2. **Bank Staff** — Onboards synthetic customers and creates accounts, processes simulated over-the-counter cash deposits and withdrawals, inspects customer account status, and submits freeze/unfreeze requests.
3. **Administrator** — Reviews and approves/rejects staff-initiated account freeze/unfreeze requests (separation of duties / maker-checker model), manages staff accounts, inspects system-wide audit logs, and runs balance reconciliation checks.

See [docs/architecture/ROLE_PERMISSIONS.md](docs/architecture/ROLE_PERMISSIONS.md) for the proposed permission matrix.

---

## 3. Proposed Features (Prioritized)

| Tier | Description | Proposed Capabilities |
| :--- | :--- | :--- |
| **P0 — Essential** | Core functional & architectural baseline | Three-role login, customer registration & account creation, staff-operated simulated cash deposit/withdrawal, customer balance enquiry, customer transfers, transaction history, JavaFX dashboards, JDBC persistence, service validation & RBAC authorization, atomic DB transactions |
| **P1 — Differentiators** | Governance, auditability & reporting | Staff-initiated freeze/unfreeze requests, administrative approvals, transactional audit records, Java I/O statement export, balance reconciliation |
| **P2 — Optional (Research-Dependent)** | Advanced simulation extensions | Duplicate-request prevention (idempotency), explainable fraud-risk alerts, transaction disputes, savings/current account variations |

Full requirements and prioritization details are tracked in [docs/requirements/REQUIREMENTS.md](docs/requirements/REQUIREMENTS.md) and [docs/requirements/FEATURE_MATRIX.md](docs/requirements/FEATURE_MATRIX.md).

---

## 4. Proposed Technology Baseline

Aligned with the UCS553 curriculum (pending final approval in [context/DECISIONS.md](context/DECISIONS.md)):

- **Language:** Java 17
- **UI Framework:** JavaFX (desktop presentation)
- **Persistence:** SQLite via Plain JDBC (`java.sql.*`, `PreparedStatement`, explicit transaction management)
- **Build & Packaging:** Maven (to be configured in a future milestone)
- **Design Patterns & Core APIs:** Layered Architecture, DAO Pattern, DTOs, Java Collections & Generics, Custom Exceptions, Java I/O (statement exports), `java.util.logging`

---

## 5. High-Level Layered Architecture (Proposed)

```text
+-------------------------------------------------------------------+
|  Presentation Layer (JavaFX UI — Owner: R)                        |
|  com.tiet.banking.ui.{common, auth, customer, staff, admin}       |
+-------------------------------------------------------------------+
                                  |  uses DTOs & Service contracts
                                  v
+-------------------------------------------------------------------+
|  Business Service & Security Layer (Owner: S)                     |
|  com.tiet.banking.{service, security, dto, exception, model}      |
+-------------------------------------------------------------------+
                                  |  uses DAO & Transaction boundaries
                                  v
+-------------------------------------------------------------------+
|  Persistence Layer (Plain JDBC & SQLite — Owner: P)               |
|  com.tiet.banking.{dao, dao.jdbc, db} -> data/*.db (gitignored)   |
+-------------------------------------------------------------------+
```

Cross-cutting packages include `bootstrap/` (wiring/startup) and `util/` (validation, Java I/O, logging helpers). See [docs/architecture/SYSTEM_ARCHITECTURE.md](docs/architecture/SYSTEM_ARCHITECTURE.md).

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
├── docs/                             # Architecture, requirements, research, testing & handoffs
│   ├── architecture/
│   │   └── diagrams/
│   ├── requirements/
│   ├── research/
│   ├── testing/
│   ├── report/
│   │   └── screenshots/
│   └── team_handoffs/
├── src/                              # Placeholder package tree (.gitkeep only in M0)
│   ├── main/java/com/tiet/banking/
│   ├── main/resources/
│   └── test/java/com/tiet/banking/
└── data/                             # Runtime SQLite storage directory (.gitkeep only; DB files ignored)
```

---

## 7. Team Responsibilities (Equal Peer Ownership)

| Member | Module Scope | Primary Owned Directories | Detailed Handoff |
| :---: | :--- | :--- | :--- |
| **S** | Banking Engine, Security & Integration | `service/`, `security/`, `exception/`, `bootstrap/`, shared `model/` & `dto/`, `test/.../service/`, `test/.../integration/` | [01_S_BANKING_ENGINE.md](docs/team_handoffs/01_S_BANKING_ENGINE.md) |
| **P** | Database, JDBC & Reliability | `db/`, `dao/`, `dao/jdbc/`, `resources/db/`, `data/`, `test/.../dao/` | [02_P_DATABASE_AND_JDBC.md](docs/team_handoffs/02_P_DATABASE_AND_JDBC.md) |
| **R** | JavaFX Presentation & UX | `ui/common/`, `ui/auth/`, `ui/customer/`, `ui/staff/`, `ui/admin/`, `resources/styles/`, `docs/report/screenshots/` | [03_R_JAVAFX_AND_UX.md](docs/team_handoffs/03_R_JAVAFX_AND_UX.md) |

---

## 8. Current Development Status

- **Current Milestone:** `M0` — Repository initialization, persistent context system, and architectural documentation scaffolding.
- **Application Code:** **Not Started** (`src/` and `data/` contain `.gitkeep` placeholders only).
- **Pending Prerequisites Before Coding (`M1`):**
  1. Review pending external Gemini Deep Research output and populate [docs/research/RESEARCH_FINDINGS.md](docs/research/RESEARCH_FINDINGS.md) and [docs/research/REGULATORY_TRACEABILITY.md](docs/research/REGULATORY_TRACEABILITY.md).
  2. Approve architectural decisions (`ADR-001` through `ADR-007`) in [context/DECISIONS.md](context/DECISIONS.md).
  3. Finalize domain models, service contracts, and SQLite schema contracts.

See [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md) and [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md).

---

## 9. Documentation Navigation

| Category | Document | Purpose |
| :--- | :--- | :--- |
| **AI Rules** | [AGENTS.md](AGENTS.md) | Mandatory rules, authority hierarchy & Git conventions for AI/human sessions |
| **Context** | [context/00_MASTER_PROJECT_CONTEXT.md](context/00_MASTER_PROJECT_CONTEXT.md) | Stable master reference for the entire project |
| **Context** | [context/PROJECT_CONTEXT.md](context/PROJECT_CONTEXT.md) | Product vision, roles, workflows, milestones & academic report link |
| **Context** | [context/TECHNICAL_CONTEXT.md](context/TECHNICAL_CONTEXT.md) | Proposed technical stack, money handling, JDBC transactions & security |
| **Context** | [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md) | Truthful snapshot of project state |
| **Context** | [context/IMPLEMENTATION_STATUS.md](context/IMPLEMENTATION_STATUS.md) | Module-by-module feature implementation tracking table |
| **Context** | [context/KNOWN_ISSUES.md](context/KNOWN_ISSUES.md) | Architectural design risks and open questions |
| **Context** | [context/DECISIONS.md](context/DECISIONS.md) | Architecture Decision Records (ADRs) |
| **Context** | [context/SESSION_LOG.md](context/SESSION_LOG.md) | Chronological development log |
| **Context** | [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md) | Exact continuation instructions for the next session |
| **Architecture** | [docs/architecture/SYSTEM_ARCHITECTURE.md](docs/architecture/SYSTEM_ARCHITECTURE.md) | Proposed layered architecture & component boundaries |
| **Architecture** | [docs/architecture/DATABASE_DESIGN.md](docs/architecture/DATABASE_DESIGN.md) | Conceptual entity design & SQLite considerations |
| **Architecture** | [docs/architecture/SERVICE_CONTRACTS.md](docs/architecture/SERVICE_CONTRACTS.md) | Provisional service & DAO interface boundaries |
| **Architecture** | [docs/architecture/ROLE_PERMISSIONS.md](docs/architecture/ROLE_PERMISSIONS.md) | RBAC & account ownership rules |
| **Architecture** | [docs/architecture/TRANSACTION_MODEL.md](docs/architecture/TRANSACTION_MODEL.md) | Financial operation lifecycle & ACID guarantees |
| **Requirements** | [docs/requirements/REQUIREMENTS.md](docs/requirements/REQUIREMENTS.md) | Functional, non-functional & UCS553 curriculum alignment |
| **Requirements** | [docs/requirements/FEATURE_MATRIX.md](docs/requirements/FEATURE_MATRIX.md) | P0 / P1 / P2 prioritized feature matrix |
| **Research** | [docs/research/RESEARCH_FINDINGS.md](docs/research/RESEARCH_FINDINGS.md) | Placeholder for pending external research synthesis |
| **Research** | [docs/research/REGULATORY_TRACEABILITY.md](docs/research/REGULATORY_TRACEABILITY.md) | Placeholder table for Indian banking/RBI conceptual traceability |
| **Testing** | [docs/testing/TEST_PLAN.md](docs/testing/TEST_PLAN.md) | Proposed unit, DAO, integration & UI verification plan (not executed) |
| **Testing** | [docs/testing/DEMO_SCENARIOS.md](docs/testing/DEMO_SCENARIOS.md) | Planned academic evaluation walkthrough scenarios |

---

## 10. Contribution Workflow

1. Never commit directly to `main`; create a short-lived branch (e.g., `feature/banking-engine`, `feature/database-jdbc`, `feature/javafx-ui`, `feature/audit-controls`, `docs/project-report`).
2. Review [AGENTS.md](AGENTS.md) and your module's handoff document before modifying code.
3. Update shared contracts (`docs/architecture/*`) and [context/DECISIONS.md](context/DECISIONS.md) before changing cross-module boundaries.
4. Open a Pull Request for teammate cross-review with reproducible verification notes and updated context files.

---

## 11. Academic Simulation Disclaimer

This software project is strictly an **educational simulation** created for academic coursework (**UCS553 — Enterprise Web Application**, TIET Patiala). All customer identities, accounts, balances, and transactions use **synthetic data and simulated funds**. Nothing in this repository represents real banking operations, real financial custody, RBI regulatory compliance, legal certification, or production-ready financial security.
