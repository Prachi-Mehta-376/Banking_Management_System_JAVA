# Software Requirements Specification (`REQUIREMENTS.md`)

> **Version:** M1 v2.0 Specification (`PROPOSED — AWAITING TEAM REVIEW`)
> **Course Context:** UCS553 — Enterprise Web Application (Thapar Institute of Engineering and Technology)

---

## 1. Purpose & Scope

This specification defines the functional and non-functional requirements for the **Banking Management System** educational simulation. Feature prioritization and module ownership appear in [FEATURE_MATRIX.md](FEATURE_MATRIX.md).

---

## 2. Functional Requirements

### 2.1 Bootstrap, Authentication & Role Provisioning (`FR-AUTH`)
- `FR-AUTH-01` (**P0**): On first launch (when zero `ADMIN` accounts exist), the application must present a secure first-run Administrator setup screen using an operator-supplied password (no hardcoded/committed default credentials) and permanently lock setup once an `ADMIN` exists.
- `FR-AUTH-02` (**P0**): Public customer registration must create a `CUSTOMER` identity paired atomically with one `ACTIVE` bank account starting at `0` paise (`₹0.00`); callers cannot specify `STAFF` or `ADMIN` roles.
- `FR-AUTH-03` (**P0**): Users across all three roles (`CUSTOMER`, `STAFF`, `ADMIN`) must authenticate with normalized email + password and route to role-specific JavaFX dashboards.
- `FR-AUTH-04` (**P0**): Passwords must be stored using per-user random salt and salted cryptographic hashing (`PBKDF2WithHmacSHA256`), never plaintext.
- `FR-AUTH-05` (**P0**): Administrators must be able to create and deactivate `STAFF` accounts (`users.is_active`), with each action recorded in `audit_events`.
- `FR-AUTH-06` (**P0**): Users must be able to log out cleanly, clearing the active `UserSession`.

### 2.2 Account Enquiry, Staff Search & Cash Operations (`FR-ACCT` / `FR-STAFF`)
- `FR-ACCT-01` (**P0**): Customers must be able to view their own account number, status (`ACTIVE`/`FROZEN`), and exact balance formatted from integer paise (`INV-06`).
- `FR-STAFF-01` (**P0**): Authenticated `STAFF` users must be able to search fictitious customers/accounts by name, email, or account number and view authorized account summaries and transaction history.
- `FR-STAFF-02` (**P0**): `STAFF` must be able to execute simulated cash deposits into an `ACTIVE` customer account, creating 1 `operations` row and 1 positive `transactions` row atomically.
- `FR-STAFF-03` (**P0**): `STAFF` must be able to execute simulated cash withdrawals from an `ACTIVE` customer account after verifying sufficient balance (`INV-01`), creating 1 `operations` row and 1 negative `transactions` row atomically.
- `FR-ACCT-02` (**P2 — Optional**): Multiple account types (e.g., Savings/Current) pending research and timeline review.

### 2.3 Customer Transfers & Transaction History (`FR-TXN`)
- `FR-TXN-01` (**P0**): Customers must be able to transfer simulated money from their own `ACTIVE` account to another customer's `ACTIVE` account (`source != destination`).
- `FR-TXN-02` (**P0**): All financial operations must parse monetary input via `BigDecimal`, convert strictly to positive integer paise, and reject `<= 0`, scale `> 2`, and `long` overflow (`INV-02`, `INV-12`).
- `FR-TXN-03` (**P0**): Internal transfers must create 1 `TRANSFER` row in `operations` and 2 `transactions` rows whose deltas sum to `0` (`INV-04`), committed atomically (`INV-03`).
- `FR-TXN-04` (**P0**): Customers and Staff must be able to inspect chronological (newest-first) account transaction history.
- `FR-TXN-05` (**P1**): Persistent idempotency protection via `operations.idempotency_key` to prevent duplicate execution on retries.

### 2.4 Governance, Audit & Controls (`FR-GOV`)
- `FR-GOV-01` (**P0**): Administrators must be able to view read-only `AdministrativeSummary` statistics; privileged setup and staff provisioning actions write actor-linked `audit_events` rows (`INV-10`). *(Resolved dependency: `audit_events` table and `AuditEventDao.insert` are part of the P0 core schema/persistence baseline; the Admin Audit Viewer UI is P1.)*
- `FR-GOV-02` (**P1**): Administrators must be able to inspect `audit_events` through an administrative audit viewer.
- `FR-GOV-03` (**P1**): Administrators must be able to run non-destructive balance reconciliation comparing `accounts.balance_paise` against `SUM(transactions.delta_paise)` (`INV-11`).
- `FR-GOV-04` (**P1**): `STAFF` must be able to request account `FREEZE`/`UNFREEZE`, reviewed by a different `ADMIN` (`account_action_requests`). `FROZEN` accounts reject deposits, withdrawals, and transfers (`INV-07`).
- `FR-GOV-05` (**P1**): Customers must be able to export own account transaction history to CSV using Java I/O.
- `FR-GOV-06` (**P2 — Optional**): Explainable rule-based fraud alerts and transaction dispute workflow (pending Gemini Deep Research).

---

## 3. Non-Functional Requirements

- `NFR-01` (**Precision**): Integer paise (`long` / `INTEGER`) internally and in SQLite; `BigDecimal` boundary parsing; zero `float`/`double`.
- `NFR-02` (**Atomicity**): Multi-write operations coordinated via `TransactionExecutor` on one shared JDBC `Connection` (`ADR-004`).
- `NFR-03` (**Authorization**): Role and ownership rules enforced inside services, independent of UI visibility (`ADR-005`).
- `NFR-04` (**SQL Safety**): `PreparedStatement` binding on all queries; `PRAGMA foreign_keys = ON` on every connection.
- `NFR-05` (**Usability & Simplicity**): Clear JavaFX forms, tables, and user-friendly error messages without overengineered abstractions (`AGENTS.md` Section 5).
- `NFR-06` (**Logging & Diagnostics**): Operational diagnostics recorded via `java.util.logging` without exposing plaintext passwords.
- `NFR-07` (**Testability**): Reproducible service unit tests and SQLite DAO/integration tests verifying `INV-01`–`INV-12`.

---

## 4. UCS553 Curriculum Alignment

| Syllabus Concept | Planned Evidence |
| :--- | :--- |
| **Core Java OOP** | Layered architecture, `Money` value class, domain entities, `Role`/`AccountStatus`/`OperationType` enums |
| **Collections & Generics** | Typed `List<T>`, `Optional<T>`, generic `TransactionWork<T>` callback |
| **Custom Exceptions** | Typed auth, authorization, validation, insufficient-funds, frozen-account, and persistence exceptions |
| **Plain JDBC & SQLite** | `TransactionExecutor`, `PreparedStatement`, `ResultSet` mapping, `commit`/`rollback`, `FOREIGN KEY`/`CHECK` constraints |
| **JavaFX Desktop UI** | Role-routed dashboards, forms, `TableView` transaction/search tables, CSS styling |
| **Java I/O & Logging** | P1 CSV statement export (`java.io`/`java.nio.file`) and `java.util.logging` diagnostics |
