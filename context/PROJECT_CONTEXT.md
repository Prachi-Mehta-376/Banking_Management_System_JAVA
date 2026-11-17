# Project Context (`PROJECT_CONTEXT.md`)

> **Focus:** Product Vision, Users, End-to-End Workflows, Scope Boundaries, Milestones, and Academic Report Alignment.
> **Master Reference:** [00_MASTER_PROJECT_CONTEXT.md](00_MASTER_PROJECT_CONTEXT.md)

---

## 1. Product Vision

The **Banking Management System** aims to deliver an intuitive, cohesive, and thoroughly documented desktop simulation of core retail banking operations for **UCS553 — Enterprise Web Application** at **Thapar Institute of Engineering and Technology, Patiala**.

Rather than a minimal CRUD screen over a flat file, the application models real-world operational separation between customers, branch tellers/staff, and administrative oversight—demonstrating how business logic, transactional safety, authorization boundaries, and user experience fit together in Core Java.

---

## 2. Intended Users & Three-Role Architecture

```text
+-------------------+      +-------------------+      +-------------------+
|     Customer      |      |    Bank Staff     |      |   Administrator   |
+-------------------+      +-------------------+      +-------------------+
| - Login           |      | - Login           |      | - Login           |
| - View own balance|      | - Onboard customer|      | - Approve/reject  |
| - View history    |      | - Open account    |      |   freeze/unfreeze |
| - Transfer funds  |      | - Cash deposit /  |      | - Manage staff    |
| - Export statement|      |   withdrawal sim  |      | - View audit logs |
|   (proposed P1)   |      | - Request freeze  |      | - Reconcile ledger|
+-------------------+      +-------------------+      +-------------------+
```

### Role Summaries
1. **Customer:** Self-service portal restricted strictly to accounts owned by the authenticated customer. Cannot alter account states directly or inspect other customers' data.
2. **Bank Staff:** Branch counter operations. Registers synthetic customers, opens accounts, processes simulated cash deposits/withdrawals (the mechanism by which simulated liquidity enters/exits customer accounts), and submits freeze/unfreeze requests with reasons.
3. **Administrator:** Governance & oversight. Does not bypass maker-checker controls to arbitrarily invent customer balances; instead manages staff credentials/status, adjudicates freeze/unfreeze requests, reviews audit trails, and verifies system balance conservation.

See [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md) for the proposed role matrix.

---

## 3. Core Proposed User Workflows

| Workflow ID | Role(s) | Summary Flow (Conceptual Proposal) | Priority |
| :--- | :--- | :--- | :---: |
| **WF-01** | All Roles | Authenticate via login screen -> service verifies credentials & role -> JavaFX routes to role-specific dashboard. | `P0` |
| **WF-02** | Bank Staff | Register synthetic customer profile -> create simulated account -> persist customer & account atomically -> display assigned account number. | `P0` |
| **WF-03** | Bank Staff | Select target active account -> enter simulated cash deposit/withdrawal amount & reference -> service validates -> atomic balance update + ledger entry + audit record. | `P0` |
| **WF-04** | Customer | View dashboard of owned accounts -> inspect balance & filterable transaction history (`TableView`). | `P0` |
| **WF-05** | Customer | Initiate internal transfer (source owned account -> destination account, positive amount) -> service checks ownership, active status, sufficient funds -> atomic debit/credit + immutable ledger entries. | `P0` |
| **WF-06** | Staff -> Admin | Staff submits freeze/unfreeze request with justification -> Admin reviews pending queue -> Admin approves/rejects -> account status updates + audit log recorded. | `P1` |
| **WF-07** | Customer / Admin | Customer exports simulated account statement via Java I/O; Admin runs ledger vs. balance reconciliation check and inspects audit trail. | `P1` |
| **WF-08** | Customer / Staff | Optional duplicate-request prevention, explainable risk alerts, dispute tracking, and account-type policy checks (pending research). | `P2` |

---

## 4. Functional Objectives & Project Scope

### Objectives
- Demonstrate clean OOP, layered modularity, and parallel teammate collaboration (`S`, `P`, `R`).
- Enforce ACID properties during financial transfers so partial debits/credits can never occur.
- Enforce service-layer authorization so security never relies exclusively on hiding JavaFX buttons.
- Maintain traceable documentation linking requirements, ADRs, tests, and the final academic report.

### Explicit Exclusions
- No external banking network connectivity (no UPI, NEFT, IMPS, SWIFT, or card networks).
- No real identity verification APIs (synthetic data only).
- No web server, REST API deployment, Spring Boot, Hibernate, or JavaScript frontend.
- No production regulatory compliance claims (e.g., RBI audit sign-off or PCI-DSS certification).
- No multi-user distributed concurrency over a network server (local desktop SQLite simulation only).

---

## 5. Development Milestones

| Milestone | Scope | Exit Criteria | Current State |
| :--- | :--- | :--- | :--- |
| **M0 — Foundation & Context** | Repository structure, `.gitignore`, `AGENTS.md`, `README.md`, `context/`, `docs/` scaffolding | All scaffolding docs verified, zero code/forbidden artifacts, team aligned | **In Final Verification (Working Tree)** |
| **M1 — Research & Contract Freeze** | Integrate Gemini Deep Research; approve ADRs, domain models, DTOs, service interfaces & SQLite schema | Contracts signed off by `S`, `P`, `R`; `CURRENT_STATUS` updated | `NOT STARTED` |
| **M2 — Persistence & Banking Engine** | Maven `pom.xml`, SQLite schema/DB setup (`P`), DAO implementations (`P`), domain services & RBAC (`S`) | Unit & DAO tests executed and passing | `NOT STARTED` |
| **M3 — JavaFX UI & Integration** | Role dashboards & screens (`R`), mock-to-service wiring (`S` + `R`), end-to-end integration tests | P0 workflows functional end-to-end | `NOT STARTED` |
| **M4 — P1/P2 Polish & Verification** | Freeze/unfreeze maker-checker, audit viewer, statement export, reconciliation, optional P2 items | Full test plan executed & recorded honestly | `NOT STARTED` |
| **M5 — Academic Report & Demo** | Screenshots (`docs/report/screenshots/`), demo script dry run, official UCS553 report compilation | Report & demo ready for evaluation | `NOT STARTED` |

---

## 6. Relationship with the Official Academic Report

Artifacts produced across `docs/` feed directly into the final UCS553 submission stored under `docs/report/`:
- Architecture rationale and conceptual diagrams -> System Design section ([../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md)).
- Requirements & feature matrix -> Software Requirements Specification section ([../docs/requirements/REQUIREMENTS.md](../docs/requirements/REQUIREMENTS.md)).
- Research synthesis & traceability -> Literature / Domain Compliance Context section ([../docs/research/RESEARCH_FINDINGS.md](../docs/research/RESEARCH_FINDINGS.md)).
- Test plan, execution evidence, and UI screenshots -> Testing & Results section ([../docs/testing/TEST_PLAN.md](../docs/testing/TEST_PLAN.md) and `docs/report/screenshots/`).
