# Software Requirements Specification (`REQUIREMENTS.md`)

> **Status:** `PROPOSED BASELINE — PENDING M1 SIGN-OFF`
> **Course Context:** UCS553 — Enterprise Web Application (TIET, Patiala)

---

## 1. Purpose & Scope

This document defines proposed functional and non-functional requirements for the **Banking Management System** educational simulation. Priorities and optional P2 extensions are summarized in [FEATURE_MATRIX.md](FEATURE_MATRIX.md).

---

## 2. Functional Requirements

### 2.1 Authentication & Session Management (`FR-AUTH`)
- `FR-AUTH-01` (**P0**): Users must authenticate with username and password before accessing any banking screen.
- `FR-AUTH-02` (**P0**): The system must support three roles (`CUSTOMER`, `STAFF`, `ADMIN`) and route each user to their role-specific JavaFX workspace upon login.
- `FR-AUTH-03` (**P0**): Passwords must be stored as salted cryptographic hashes rather than plaintext.
- `FR-AUTH-04` (**P0**): Users must be able to log out cleanly, clearing the active session state.

### 2.2 Customer Onboarding & Account Creation (`FR-CUST` / `FR-ACCT`)
- `FR-CUST-01` (**P0**): Bank Staff must be able to register a synthetic customer profile alongside a linked `CUSTOMER` login identity.
- `FR-ACCT-01` (**P0**): Bank Staff must be able to open a new simulated bank account for an existing synthetic customer, generating a unique account number.
- `FR-ACCT-02` (**P0**): Customers must be able to view balances and status details strictly for accounts they own.
- `FR-ACCT-03` (**P2 — Optional**): Support Savings vs. Current simulated account variants (subject to research & timeline).

### 2.3 Simulated Cash Operations & Customer Transfers (`FR-TXN`)
- `FR-TXN-01` (**P0**): Bank Staff must be able to process simulated cash deposits into an active customer account.
- `FR-TXN-02` (**P0**): Bank Staff must be able to process simulated cash withdrawals from an active customer account after balance validation.
- `FR-TXN-03` (**P0**): Customers must be able to transfer simulated funds from one of their own active accounts to another valid active account.
- `FR-TXN-04` (**P0**): All financial operations must reject zero/negative amounts, inactive/frozen accounts, self-transfers (same source and destination), and insufficient funds.
- `FR-TXN-05` (**P0**): Authorized users must be able to inspect chronological transaction history for an eligible account.
- `FR-TXN-06` (**P2 — Optional**): Prevent accidental duplicate transfer submissions (idempotency check) and surface explainable rule-based risk flags.

### 2.4 Governance, Audit & Reporting (`FR-GOV` — Primarily P1)
- `FR-GOV-01` (**P1**): Bank Staff must be able to submit account freeze or unfreeze requests with a written reason.
- `FR-GOV-02` (**P1**): Administrators must be able to approve or reject pending freeze/unfreeze requests; requesters cannot approve their own requests.
- `FR-GOV-03` (**P1**): Administrators must be able to manage Bank Staff accounts (create/enable/disable).
- `FR-GOV-04` (**P1**): Sensitive operations (logins, deposits, withdrawals, transfers, freeze approvals, authorization denials) must generate inspectable audit records.
- `FR-GOV-05` (**P1**): Authorized users must be able to export simulated account statements to a local file via Java I/O.
- `FR-GOV-06` (**P1**): Administrators must be able to execute a balance reconciliation check verifying account balances match immutable ledger records.
- `FR-GOV-07` (**P2 — Optional**): Customer-submitted transaction dispute tracking workflow.

---

## 3. Non-Functional Requirements

- `NFR-01` (**Financial Precision**): No `float` or `double` types for balances or monetary amounts (`ADR-003`).
- `NFR-02` (**Transactional Consistency**): Multi-step financial mutations must execute atomically in JDBC transactions with full rollback on error (`ADR-004`).
- `NFR-03` (**Authorization Integrity**): Role and ownership rules must be enforced in the business service layer, independent of UI hiding (`ADR-005`).
- `NFR-04` (**SQL Injection Resistance**): All JDBC queries must use parameterized `PreparedStatement`s.
- `NFR-05` (**Explainability & Usability**): JavaFX interfaces must present clear validation messages, tabular transaction views, and intuitive navigation across roles.
- `NFR-06` (**Auditability & Diagnostics**): Operational diagnostics logged via `java.util.logging` without exposing plaintext secrets.
- `NFR-07` (**Academic Maintainability**): Clean layered code with unit, DAO, and integration test coverage under standard Maven layout.
- `NFR-08` (**Simulation Safety**): Strictly synthetic identities and simulated funds; offline local execution without external API dependencies.

---

## 4. UCS553 Curriculum Alignment

| Syllabus Concept | Planned Application Evidence |
| :--- | :--- |
| **Object-Oriented Design** | Encapsulated domain models, role/service abstractions, clean package layering |
| **Collections & Generics** | Typed `List<T>`, `Map<K,V>`, `Optional<T>` across DAOs, services, and JavaFX `TableView` models |
| **Exception Handling** | Custom checked/unchecked exception hierarchy (`exception/`) mapped cleanly to UI feedback |
| **JDBC & Relational DB** | Plain JDBC `Connection`, `PreparedStatement`, `ResultSet`, foreign keys, `commit`/`rollback` |
| **Desktop GUI (JavaFX)** | Multi-role views, forms, observable `TableView`s, CSS styling |
| **Java I/O** | Statement export writer (`util/`) and SQLite schema/config loading |
| **Logging** | `java.util.logging` operational diagnostics across layers |
