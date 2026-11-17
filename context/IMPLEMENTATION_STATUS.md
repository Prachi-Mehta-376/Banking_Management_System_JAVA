# Implementation Status (`IMPLEMENTATION_STATUS.md`)

> **Truthfulness Rule:** Every implementation item starts as `NOT STARTED` until actual source code and tests exist in the repository.

---

## 1. Status Legend

| Status | Meaning |
| :--- | :--- |
| `NOT STARTED` | No code or artifacts exist yet (design/requirements only) |
| `IN PROGRESS` | Actively being implemented on a feature branch |
| `IMPLEMENTED` | Code written, compiles cleanly, awaiting comprehensive testing |
| `TESTED` | Automated or documented verification executed and passing |
| `VERIFIED` | Cross-reviewed, verified against acceptance criteria, merged to `main` |
| `BLOCKED` | Cannot proceed due to unresolved dependency, contract, or decision |
| `DEFERRED` | Intentionally postponed beyond current milestone scope |

---

## 2. Module & Feature Implementation Matrix

| ID | Module / Feature | Priority | Owner | Dependencies | Verification Criteria | Status |
| :--- | :--- | :---: | :---: | :--- | :--- | :---: |
| **MOD-00** | Maven `pom.xml` & Project Bootstrap | `P0` | Shared (`S`+`P`+`R`) | M1 contract sign-off | `mvn test-compile` succeeds cleanly | `NOT STARTED` |
| **MOD-01** | Core Domain Models & Status Enums (`model/`) | `P0` | `S` (w/ `P`) | `ADR-003` (Money), DB schema | Immutable/validated models unit-tested | `NOT STARTED` |
| **MOD-02** | Service DTOs & Exception Hierarchy (`dto/`, `exception/`) | `P0` | `S` (w/ `R`) | MOD-01, Service contracts | Clean mapping & exception propagation checks | `NOT STARTED` |
| **MOD-03** | SQLite Connection, Pragmas & Tx Manager (`db/`) | `P0` | `P` | `ADR-004` (Tx Coordination) | Connection lifecycle & rollback verified in DAO tests | `NOT STARTED` |
| **MOD-04** | SQLite Schema & Seed/Init Script (`resources/db/`) | `P0` | `P` | MOD-01, `DATABASE_DESIGN.md` | Schema initializes cleanly with FK & CHECK constraints | `NOT STARTED` |
| **MOD-05** | JDBC DAOs (User, Customer, Account, Transaction) (`dao/`) | `P0` | `P` | MOD-01, MOD-03, MOD-04 | CRUD, PreparedStatements & constraint tests pass | `NOT STARTED` |
| **MOD-06** | Auth, Password Hashing & Session Security (`security/`) | `P0` | `S` | MOD-05 (`UserDao`) | Login hash check, role & ownership guard tests pass | `NOT STARTED` |
| **MOD-07** | Customer & Account Services (`service/`) | `P0` | `S` | MOD-05, MOD-06 | Registration, account creation, balance query tests pass | `NOT STARTED` |
| **MOD-08** | Deposit, Withdrawal & Atomic Transfer Engine (`service/`) | `P0` | `S` | MOD-03, MOD-05, MOD-06 | Atomic commit, insufficient funds & frozen rollback pass | `NOT STARTED` |
| **MOD-09** | JavaFX App Shell & Login/Auth UI (`ui/common`, `ui/auth`) | `P0` | `R` | MOD-02, MOD-06 | Three-role routing & validation feedback verified | `NOT STARTED` |
| **MOD-10** | Customer Dashboard & Transfer UI (`ui/customer`) | `P0` | `R` | MOD-07, MOD-08, MOD-09 | Balance, `TableView` history & transfer flow verified | `NOT STARTED` |
| **MOD-11** | Bank Staff Dashboard & Cash Ops UI (`ui/staff`) | `P0` | `R` | MOD-07, MOD-08, MOD-09 | Onboarding, deposit & withdrawal flows verified | `NOT STARTED` |
| **MOD-12** | Freeze/Unfreeze Workflow & Admin Approval (`service/`, `dao/`, `ui/admin`) | `P1` | `S`+`P`+`R` | MOD-05, MOD-08 | Staff request + Admin approval/rejection verified | `NOT STARTED` |
| **MOD-13** | Transactional Audit Log & Admin Viewer | `P1` | `S`+`P`+`R` | MOD-05, MOD-08 | Sensitive actions produce immutable audit entries | `NOT STARTED` |
| **MOD-14** | Statement Export (Java I/O) & Reconciliation Check | `P1` | `S`+`R` | MOD-07, MOD-08 | Valid exported file generated; sum matches ledger | `NOT STARTED` |
| **MOD-15** | Logging (`java.util.logging`) & Validation Utils (`util/`) | `P0` | `S` (Shared) | MOD-00 | Logs written cleanly without leaking credentials | `NOT STARTED` |
| **MOD-16** | End-to-End Integration & UI Walkthrough Verification | `P0` | Shared | MOD-08..MOD-11 | All P0 scenarios in `TEST_PLAN.md` pass | `NOT STARTED` |
| **MOD-17** | P2 Research Extensions (Idempotency, Risk Alerts, Disputes, Account Variants) | `P2` | TBD | Gemini Deep Research review | Scoped and tested if promoted to active milestone | `NOT STARTED` |
