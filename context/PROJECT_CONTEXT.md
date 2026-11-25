# Project Context (`PROJECT_CONTEXT.md`)

> **Focus:** Product Vision, Users, End-to-End Workflows, Scope Boundaries, Milestones & Academic Report Link.
> **Master Reference:** [00_MASTER_PROJECT_CONTEXT.md](00_MASTER_PROJECT_CONTEXT.md)

---

## 1. Product Vision

The **Banking Management System** is a third-year undergraduate Core Java desktop simulation for **UCS553 — Enterprise Web Application** (TIET, Patiala). It prioritizes correctness, clean separation of concerns, and student explainability over unnecessary enterprise boilerplate.

---

## 2. Intended Users & Three-Role Architecture

```text
+-----------------------+      +-----------------------+      +-----------------------+
|       Customer        |      |      Bank Staff       |      |     Administrator     |
+-----------------------+      +-----------------------+      +-----------------------+
| - Public registration |      | - Admin-provisioned   |      | - First-run secure    |
|   (1 account @ ₹0.00) |      |   credentials login   |      |   initial setup       |
| - View own balance    |      | - Search customers    |      | - Provision/deactivate|
| - View own history    |      | - Simulated cash      |      |   Staff accounts      |
| - Transfer to active  |      |   deposit/withdrawal  |      | - View admin stats    |
|   customer account    |      | - Inspect customer    |      | - View audits & run   |
| - Export CSV (P1)     |      |   account & history   |      |   reconciliation (P1) |
|                       |      | - Request freeze (P1) |      | - Review freeze (P1)  |
+-----------------------+      +-----------------------+      +-----------------------+
```

For full permissions, first-run bootstrap rules, and frozen-account restrictions, see [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md).

---

## 3. Core M1 Proposed User Workflows

| Workflow | Role(s) | Summary Flow (Proposed) | Tier |
| :--- | :--- | :--- | :---: |
| **WF-01** | First-Run Operator | On empty DB (`COUNT(ADMIN) == 0`), complete Initial Admin Setup with user-supplied password -> locked permanently afterward. | `P0` |
| **WF-02** | Admin | Log in as `ADMIN` -> provision/deactivate `STAFF` accounts (`audit_events` logged) -> inspect `AdministrativeSummary`. | `P0` |
| **WF-03** | Public -> Customer | Self-register as `CUSTOMER` -> creates `users` + `1` `ACTIVE` `accounts` row (`₹0.00` balance) atomically. | `P0` |
| **WF-04** | Bank Staff | Log in -> search customers/accounts -> perform simulated cash deposit or withdrawal -> creates `operations` + `transactions` rows atomically. | `P0` |
| **WF-05** | Customer | View single owned account balance & newest-first transaction history -> transfer simulated money to another `ACTIVE` account. | `P0` |
| **WF-06** | Admin / Customer / Staff | P1 controls: non-destructive reconciliation, admin audit viewer, maker-checker freeze/unfreeze, CSV statement export, idempotent retries. | `P1` |
| **WF-07** | All Roles | Optional research-dependent extensions (explainable fraud alerts, disputes, multiple account types). | `P2` |

---

## 4. Scope Boundaries & Explicit Exclusions

- **Included (v1):** Single local SQLite DB, fictitious users, 1 bank account per customer starting at `0` paise, `ACTIVE`/`FROZEN` statuses, integer-paise arithmetic, atomic `TransactionExecutor` updates, and JUnit service/DB tests.
- **Excluded:** External payment networks (UPI/NEFT/IMPS), real KYC verification, Spring/Hibernate/web deployment, account closure in P0, and regulatory compliance claims.

---

## 5. Development Milestones

| Milestone | Scope | Exit Criteria | Current State |
| :--- | :--- | :--- | :--- |
| **M0 — Foundation & Context** | Repository tree, `.gitignore`, `AGENTS.md`, `README.md`, `context/`, `docs/` | Verified and committed on `main` (`3929940`) | **Complete** |
| **M1 — Architecture v2.0 & Contracts** | Financial model (`ADR-003`/`004`/`007`/`008`), DB spec, invariants, DTOs, services, test plan | Approved by `S` as working baseline; merged via PR #1 (`5b9fad2`) | **Complete** |
| **M2 — Core Persistence & Banking Engine** | Maven `pom.xml`, 7-table SQLite schema & `TransactionExecutor`, `Money`/Security/Services | `MoneyValidationTest`, `DatabasePersistenceTest`, `AuthAndRbacTest` passing | **Complete** |
| **M3 — JavaFX UI & Integration** | JavaFX views & CSS, `AppNavigator`/`ServiceRegistry`, live SQLite integration | All P0 workflows verified in `BankingEngineIntegrationTest` & live GUI test | **Complete** |
| **M4 — P1 Controls & Verification** | Reconciliation, audit viewer, freeze/unfreeze maker-checker, CSV export, idempotency | All P1 controls implemented and verified (`12/12` passing) | **Complete** |
| **M5 — Academic Report & Team Release** | UI screenshots (`docs/report/screenshots/`), Maven Wrapper, portable README, UCS553 report evidence | Prepared and verified for teammate onboarding (`main`) | **Complete** |

---

## 6. Relationship with the Official Academic Report

Documentation in `docs/architecture/`, `docs/requirements/`, `docs/research/`, and `docs/testing/` feeds directly into `docs/report/` (including verified M5 screenshots in `docs/report/screenshots/`).
