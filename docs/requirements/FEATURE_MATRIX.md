# Feature Matrix (`FEATURE_MATRIX.md`)

> **Status Notice:** Priorities below are **proposed planning categories**, not guaranteed commitments for every P1/P2 item. Every feature starts as `NOT STARTED`.

---

## 1. Priority Tier Definitions

- **P0 — Essential:** Core functional and architectural requirements required for a working, verifiable UCS553 submission.
- **P1 — Differentiators:** Governance, auditability, and operational integrity features that elevate engineering quality once P0 is stable.
- **P2 — Optional (Research-Dependent):** Advanced enhancements gated on Gemini Deep Research review and remaining schedule capacity.

---

## 2. Prioritized Feature Matrix

### P0 — Essential Features

| ID | Feature | Target Role(s) | Primary Owner(s) | Dependencies | Current Status |
| :---: | :--- | :--- | :---: | :--- | :---: |
| `F-P0-01` | Three-role login & session context (`Customer`, `Bank Staff`, `Administrator`) | All Roles | `S` + `P` + `R` | `UserDao`, `AuthService`, Login UI | `NOT STARTED` |
| `F-P0-02` | Synthetic customer registration & simulated account creation | Bank Staff | `S` + `P` + `R` | `CustomerDao`, `AccountDao`, Staff UI | `NOT STARTED` |
| `F-P0-03` | Staff-operated simulated cash deposit & withdrawal | Bank Staff | `S` + `P` + `R` | Transaction coordinator, `AccountDao`, `TransactionDao` | `NOT STARTED` |
| `F-P0-04` | Customer balance enquiry (owned accounts only) | Customer | `S` + `P` + `R` | Ownership check, `AccountDao`, Customer UI | `NOT STARTED` |
| `F-P0-05` | Customer account-to-account transfers | Customer | `S` + `P` + `R` | Ownership check, atomic DB transaction | `NOT STARTED` |
| `F-P0-06` | Chronological transaction history (`TableView`) | Customer, Staff, Admin | `S` + `P` + `R` | `TransactionDao`, immutable ledger entries | `NOT STARTED` |
| `F-P0-07` | Role-specific JavaFX dashboards & navigation shell | All Roles | `R` (w/ `S` DTOs) | `SERVICE_CONTRACTS.md` | `NOT STARTED` |
| `F-P0-08` | SQLite plain JDBC persistence (`PreparedStatement`, DAO pattern) | System | `P` | `DATABASE_DESIGN.md`, `db/` | `NOT STARTED` |
| `F-P0-09` | Service-layer input validation & RBAC/ownership authorization | System | `S` | `ROLE_PERMISSIONS.md`, `exception/` | `NOT STARTED` |
| `F-P0-10` | Database transaction consistency (commit / rollback) | System | `S` + `P` | `ADR-004`, `TRANSACTION_MODEL.md` | `NOT STARTED` |

### P1 — Differentiator Features

| ID | Feature | Target Role(s) | Primary Owner(s) | Dependencies | Current Status |
| :---: | :--- | :--- | :---: | :--- | :---: |
| `F-P1-01` | Staff-initiated freeze / unfreeze requests | Bank Staff | `S` + `P` + `R` | `FreezeRequestDao`, account lifecycle rules | `NOT STARTED` |
| `F-P1-02` | Administrative approvals (maker-checker + staff management) | Administrator | `S` + `P` + `R` | `F-P1-01`, `ROLE_PERMISSIONS.md` | `NOT STARTED` |
| `F-P1-03` | Immutable transactional & security audit records | Administrator | `S` + `P` + `R` | `AuditLogDao`, `RISK-06` resolution | `NOT STARTED` |
| `F-P1-04` | Simulated account statement export (Java I/O) | Customer, Staff, Admin | `S` + `R` | `F-P0-06`, Java I/O helper (`util/`) | `NOT STARTED` |
| `F-P1-05` | Balance reconciliation check (ledger vs. stored balances) | Administrator | `S` + `P` + `R` | `ADR-007`, `TransactionDao` aggregates | `NOT STARTED` |

### P2 — Optional Research-Dependent Features

| ID | Feature | Target Role(s) | Primary Owner(s) | Dependencies | Current Status |
| :---: | :--- | :--- | :---: | :--- | :---: |
| `F-P2-01` | Duplicate-request prevention (transfer idempotency) | Customer, Staff | `S` + `P` | Gemini Deep Research review & P0 completion | `NOT STARTED` |
| `F-P2-02` | Explainable rule-based fraud-risk alerts | Staff, Admin | `S` + `P` + `R` | Gemini Deep Research review & audit logs | `NOT STARTED` |
| `F-P2-03` | Transaction dispute submission & review | Customer, Staff, Admin | `S` + `P` + `R` | Gemini Deep Research review & P1 completion | `NOT STARTED` |
| `F-P2-04` | Savings / Current account policy variations | Staff, Customer | `S` + `P` + `R` | M1/Research review & P0/P1 completion | `NOT STARTED` |
