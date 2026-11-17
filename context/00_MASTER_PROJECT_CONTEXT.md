# Master Project Context (`00_MASTER_PROJECT_CONTEXT.md`)

> **Authority Level:** Stable Master Reference for Project Identity, Scope, and Boundaries.
> **Current Phase:** **M0 — Documentation & Architectural Scaffolding Only (No Application Code Written)**

---

## 1. Project Identity

| Attribute | Detail |
| :--- | :--- |
| **Project Title** | Banking Management System |
| **Course** | UCS553 — Enterprise Web Application |
| **Institution** | Thapar Institute of Engineering and Technology (TIET), Patiala |
| **Application Type** | Core Java Desktop Application (Java 17 + JavaFX + Plain JDBC + SQLite) |
| **Repository Role** | Shared collaborative workspace for engineering, documentation, and academic reporting |
| **Domain Classification** | Educational Banking Simulation (Synthetic Data & Simulated Funds Only) |

---

## 2. Problem Statement & Academic Objective

Traditional university banking demos often conflate UI logic with raw SQL, store money in imprecise floating-point types, omit role separation, or skip transactional guarantees during transfers.

The objective of this project is to design and build a **robust, explainable, and academically defensible Banking Management System simulation** aligned with the **UCS553** syllabus. Rather than relying on heavy external enterprise frameworks (e.g., Spring Boot or Hibernate), the system demonstrates enterprise engineering discipline directly in **Core Java**:

- Clean separation of concerns across Presentation, Business Service, Security, and Persistence layers.
- Explicit ACID transaction management using plain JDBC over SQLite.
- Role-based access control (RBAC) and customer ownership enforcement.
- Immutable transaction recording, audit trails, and explainable administrative governance controls.

---

## 3. Confirmed Requirements vs. Proposed Capabilities

To prevent premature lock-in before architecture sign-off and research review, requirements are explicitly separated into **Confirmed Core Baseline** and **Proposed / Pending Finalization**:

### Confirmed Core Baseline
- Desktop Java 17 application built around a three-role model: **Customer**, **Bank Staff**, and **Administrator**.
- Local SQLite persistence accessed through **plain JDBC** and the **DAO pattern** (no ORM).
- Core banking simulation capabilities: customer onboarding/account creation, staff-operated cash deposit/withdrawal simulation, customer balance inquiry, customer account-to-account transfers, and transaction history.
- Strictly **synthetic customer data** and **simulated currency**; no real banking integration or legal compliance claims.
- Clear teammate module ownership across **S**, **P**, and **R**.

### Proposed Capabilities (Provisional Pending M1 Sign-Off & Research Review)
- **P1 Governance & Reliability:** Staff-initiated account freeze/unfreeze requests requiring Administrator approval (maker-checker separation), structured audit logs, Java I/O statement export, and balance reconciliation checks.
- **P2 Research-Dependent Extensions:** Duplicate-request (idempotency) prevention, explainable rule-based fraud-risk alerts, transaction dispute workflow, and savings/current account policy variations.
- **Technical Proposals Pending Finalization:** Exact monetary persistence format (integer minor units vs. fixed decimal representation), exact transaction boundary coordination pattern (`Connection` / Unit-of-Work), and specific SQLite schema constraints.

---

## 4. Intended Users and Three-Role Model

1. **Customer** — End user who holds one or more simulated bank accounts; can log in, inspect owned balances and transaction history, transfer funds from owned active accounts, and (proposed P1) export statements.
2. **Bank Staff** — Branch operations user; can register synthetic customers, open simulated accounts, execute simulated cash deposits and withdrawals, inspect customer account details, and (proposed P1) initiate freeze/unfreeze requests.
3. **Administrator** — Governance and oversight user; manages staff accounts, reviews and approves/rejects freeze/unfreeze requests (preventing unilateral staff freezes), inspects system-wide audit logs, and runs balance reconciliation.

Detailed role boundaries are proposed in [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md).

---

## 5. Proposed Technology Baseline & UCS553 Curriculum Alignment

| Technology / Concept | Role in System | UCS553 Curriculum Alignment | Status |
| :--- | :--- | :--- | :--- |
| **Java 17** | Core implementation language | Core OOP, encapsulation, inheritance, polymorphism | `PROVISIONAL` |
| **JavaFX** | Desktop GUI presentation layer | Rich client desktop UI, event handling, `TableView`, CSS styling | `PROVISIONAL` |
| **Plain JDBC + SQLite** | Relational persistence & ACID transactions | `Connection`, `PreparedStatement`, `ResultSet`, `commit`/`rollback` | `PROVISIONAL` |
| **DAO Design Pattern** | Decouples SQL from business services | Enterprise layered architecture & persistence abstraction | `PROVISIONAL` |
| **Collections & Generics** | Account/transaction lists, generic DAO/DTO contracts | Type safety, `List`, `Map`, `Optional` | `PROVISIONAL` |
| **Custom Exceptions** | Domain errors (insufficient funds, frozen account, auth failure) | Structured error handling & propagation | `PROVISIONAL` |
| **Java I/O** | Statement exports & configuration reading | Stream/writer-based file I/O | `PROVISIONAL` |
| **`java.util.logging`** | Diagnostic & operational application logging | Standard JDK logging infrastructure | `PROVISIONAL` |
| **Maven** | Build, dependency & test lifecycle management | Standard Java project structure & build automation | `PROVISIONAL` |

**Explicit Technology Exclusions:** Spring Boot, Hibernate/JPA, React, microservices, external cloud databases, and real payment gateways are explicitly excluded to stay strictly aligned with UCS553.

---

## 6. High-Level Architecture Overview (Proposed)

The system follows a strict **three-tier layered architecture** with unidirectional dependencies:

1. **Presentation Layer (`com.tiet.banking.ui.*`)** — JavaFX views/controllers consuming DTOs and calling service interfaces. Must never directly access DAOs or JDBC.
2. **Business Service & Security Layer (`com.tiet.banking.service.*`, `security.*`, `dto.*`, `exception.*`)** — Enforces business rules, input validation, RBAC, ownership verification, and atomic transaction orchestration.
3. **Persistence Layer (`com.tiet.banking.dao.*`, `dao.jdbc.*`, `db.*`)** — Manages SQLite connections, parameterized SQL queries, and entity mapping.
4. **Domain & Cross-Cutting (`com.tiet.banking.model.*`, `util.*`, `bootstrap.*`)** — Core entities, enums, logging/file I/O utilities, and application startup wiring.

See [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md).

---

## 7. Team Members & Peer Responsibilities

The team consists of **three equal peers** (no hierarchical team lead):

- **S — Banking Engine, Security & Integration:** Business services, authentication/RBAC, financial rules, validation, exception hierarchy, bootstrap wiring, service & integration tests. ([01_S_BANKING_ENGINE.md](../docs/team_handoffs/01_S_BANKING_ENGINE.md))
- **P — Database, JDBC & Reliability:** SQLite schema, connection & transaction infrastructure, DAO interfaces & JDBC implementations, DB constraints, persistence tests. ([02_P_DATABASE_AND_JDBC.md](../docs/team_handoffs/02_P_DATABASE_AND_JDBC.md))
- **R — JavaFX Presentation & UX:** JavaFX screens across roles, forms, `TableView` displays, CSS styling, user feedback, mock-backed UI prototyping, screenshots & UI documentation. ([03_R_JAVAFX_AND_UX.md](../docs/team_handoffs/03_R_JAVAFX_AND_UX.md))

---

## 8. Confirmed Scope Boundaries & Pending Decisions

### Scope Boundaries
- **In Scope:** Local desktop educational simulation using synthetic records, single-node SQLite persistence, role-based workflows, transactional consistency, and explainable governance/audit trails.
- **Out of Scope:** Live external network transfers (UPI/NEFT/RTGS/IMPS), real KYC verification (Aadhaar/PAN APIs), production cryptography/HSMs, multi-server clustering, and real regulatory certification.

### Pending Architecture Decisions & External Dependencies
- **Gemini Deep Research Review:** External research output on Indian banking/RBI conceptual alignment and fraud-risk controls has not yet been reviewed; `P2` features and regulatory traceability tables remain open placeholders.
- **Open ADRs:** `ADR-001` through `ADR-007` in [DECISIONS.md](DECISIONS.md) require formal team agreement before M1 contract freeze.
- **Open Design Risks:** Transaction coordination ownership, RBAC enforcement boundaries, SQLite concurrency/security limits, and account lifecycle states documented in [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

---

## 9. Authoritative Document Map

- **Agent & Coding Rules:** [../AGENTS.md](../AGENTS.md)
- **Project Vision & Workflows:** [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md)
- **Technical Foundation:** [TECHNICAL_CONTEXT.md](TECHNICAL_CONTEXT.md)
- **Current Progress:** [CURRENT_STATUS.md](CURRENT_STATUS.md) & [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)
- **Architectural Decisions & Risks:** [DECISIONS.md](DECISIONS.md) & [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
- **Session Continuity:** [SESSION_LOG.md](SESSION_LOG.md) & [SESSION_HANDOFF.md](SESSION_HANDOFF.md)
- **Architecture & Contracts:** [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md), [DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md), [ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md), [TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md)
- **Requirements & Testing:** [../docs/requirements/REQUIREMENTS.md](../docs/requirements/REQUIREMENTS.md), [FEATURE_MATRIX.md](../docs/requirements/FEATURE_MATRIX.md), [../docs/testing/TEST_PLAN.md](../docs/testing/TEST_PLAN.md)
- **Research & Traceability:** [../docs/research/RESEARCH_FINDINGS.md](../docs/research/RESEARCH_FINDINGS.md), [REGULATORY_TRACEABILITY.md](../docs/research/REGULATORY_TRACEABILITY.md)

---

## 10. Academic & Security Limitations Disclaimer

1. **Educational Simulation Only:** This system simulates banking mechanics for coursework evaluation under **UCS553**. No real funds or customer records are ever processed.
2. **No Regulatory Compliance Claims:** While conceptual inspiration may be drawn from standard banking practices (pending research review), this software makes **no claim** of RBI compliance, legal compliance, or audit certification.
3. **Local SQLite Security Limitations:** Local desktop SQLite storage is inherently accessible to the host OS user; therefore, security controls (password hashing, RBAC, audit logging) demonstrate software architecture principles rather than enterprise production hardening.
