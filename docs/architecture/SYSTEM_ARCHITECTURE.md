# System Architecture (`SYSTEM_ARCHITECTURE.md`)

> **Version:** M1 v2.0 Specification (`PROPOSED — AWAITING TEAM REVIEW`)
> **Status:** Conceptual & Contract Specification Only (No Java, SQL, or `pom.xml` Implemented)
> **Related Decisions:** `ADR-001` through `ADR-008` in [../../context/DECISIONS.md](../../context/DECISIONS.md)

---

## 1. Architectural Style & Logical Flow

The **Banking Management System** uses a **layered Core Java 17 desktop architecture** tailored to the UCS553 syllabus:

```text
JavaFX Presentation Layer (ui.* — R)
  -> Business Services (service.* — S)
    -> DAO Interfaces (dao.* — P)
      -> JDBC Implementations (dao.jdbc.* — P)
        -> SQLite Database (data/*.db)
```

### Cross-Cutting Components
- **Domain Models & Value Types (`model.*`):** `User`, `BankAccount`, `BankOperation`, `BankTransaction`, `AuditEvent`, `Role`, `AccountStatus`, `OperationType`, `Money`
- **Presentation DTOs (`dto.*`):** `UserSession`, `AccountSummary`, `TransactionReceipt`, `TransactionView`, `CustomerSearchResult`, `AdministrativeSummary`
- **Authentication & Authorization (`security.*`):** Password hashing/verification and active session/role checks enforced inside Business Services
- **Validation & Custom Exceptions (`util.*`, `exception.*`):** Monetary parsing (`BigDecimal` -> paise), input checks, and clean domain/auth/persistence exception mapping
- **Transaction Coordination (`db.*`):** `TransactionExecutor`, `TransactionWork<T>`, `BankingUnitOfWork`
- **Logging & Configuration (`util.*`, `bootstrap.*`):** `java.util.logging`, configuration defaults, and manual startup wiring

---

## 2. Package Boundaries & Dependency Rules

| Package | Owner | Responsibility | Allowed Dependencies |
| :--- | :---: | :--- | :--- |
| `com.tiet.banking.ui.*` | `R` | JavaFX dashboards, forms, tables, CSS styling, user alerts | `service`, `dto`, `model` (read-only/enums), `exception`, JavaFX |
| `com.tiet.banking.service` | `S` | Business rules, RBAC/ownership checks, orchestration, `TransactionExecutor` calls | `dao` interfaces, `db` tx contract, `security`, `model`, `dto`, `exception`, `util` |
| `com.tiet.banking.security` | `S` | Password hashing (`PBKDF2`), session holder, authorization guards | `model`, `dto`, `exception` |
| `com.tiet.banking.dao` | `P` | Persistence interfaces (`UserDao`, `AccountDao`, `OperationDao`, `TransactionDao`, `AuditEventDao`) | `model`, `exception` |
| `com.tiet.banking.dao.jdbc` | `P` | Plain JDBC `PreparedStatement` implementations on shared `Connection` | `dao`, `db`, `model`, `exception`, `java.sql` |
| `com.tiet.banking.db` | `P` | SQLite connection factory, PRAGMAs, schema initializer, `TransactionExecutor` | `model`, `exception`, `java.sql` |
| `com.tiet.banking.model` | `S` + `P` | Core domain entities, enums, `Money` value wrapper | JDK standard library only |
| `com.tiet.banking.dto` | `S` + `R` | Service-facing request/response summaries for JavaFX | `model` enums/`Money`, JDK standard library |
| `com.tiet.banking.exception` | `S` | Typed domain, validation, auth, and wrapped persistence exceptions | JDK standard library only |
| `com.tiet.banking.util` | `S` (Shared) | Input validators, CSV export (P1), `java.util.logging` setup | `exception`, `model`/`dto` |
| `com.tiet.banking.bootstrap` | `S` (Integrated) | Application startup wiring (`DB -> DAOs -> Services -> UI`) | All application layers (startup only) |

**Strict Layer Isolation:**
- `ui.*` must **never** import `dao.*`, `dao.jdbc.*`, `db.*`, or `java.sql.*`.
- `dao.*` and `dao.jdbc.*` must **never** import `service.*` or `javafx.*`, and must not enforce business RBAC rules.

---

## 3. M1 Architectural Simplifications (From M0 to M1 v2.0)

To avoid overengineering and keep the codebase proportional to a three-student UCS553 project:

1. **Unified `users` Table Instead of Separate `customers` Table:** Customer profile attributes (`full_name`, `email`) live directly in `users` with `role = 'CUSTOMER'`, paired `1:1` with `accounts`. This eliminates a redundant `CustomerDao`, `Customer` entity, and multi-table join boilerplate.
2. **Two Account States in v1 (`ACTIVE`, `FROZEN`):** Deferred `PENDING` and `CLOSED` account states out of P0/v1, keeping state validation straightforward and explainable.
3. **One Bank Account per Customer in v1:** Enforced via `UNIQUE(user_id)` on `accounts` (`AccountSummary` and `CustomerBankingService` operate directly on the customer's single account without multi-account selection complexity).
4. **No DI Container or Excessive Patterns:** Manual constructor wiring in `bootstrap/` and direct DAO interfaces without abstract base DAOs, builder hierarchies, or event buses.

---

## 4. Financial & Security Invariants (`INV-01` through `INV-12`)

> **Important:** All 12 invariants below are **intended design targets**, not verified runtime guarantees. Verification awaits M2/M3 implementation and execution of [../testing/TEST_PLAN.md](../testing/TEST_PLAN.md).

| ID | Invariant Statement | Meaning | Enforcement Layer | DB / Application Mechanism | Planned Test | Known Limitations |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `INV-01` | Account balances never become negative | No withdrawal or transfer can drive `balance_paise < 0` | Service (`S`) + DB (`P`) | Service balance check + SQL conditional `UPDATE ... WHERE balance_paise >= ?` + `CHECK (balance_paise >= 0)` | `TC-07`, `TC-12` | Local SQLite file edited externally outside the app cannot be prevented |
| `INV-02` | Monetary input must be valid, positive, and representable in paise | Reject `null`, blank, `<= 0`, scale `> 2`, and `long` overflow | Service / `Money` (`S`) | `BigDecimal` parsing (`scale <= 2`), exact `movePointRight(2).longValueExact()`, strict `> 0` check | `TC-08` | Relies on callers using `Money` parser rather than raw numbers |
| `INV-03` | Failed operations leave no partial committed financial changes | Any mid-operation failure leaves balances, operations, and journal unchanged | DB / Service (`P` + `S`) | Single shared JDBC `Connection` via `TransactionExecutor` with `setAutoCommit(false)` and `rollback()` | `TC-10`, `TC-11` | Commit uncertainty after I/O fault cannot be retried automatically in P0 |
| `INV-04` | Internal transfers have equal and opposite account deltas | For a `TRANSFER` operation, source delta + destination delta = `0` | Service (`S`) | Service creates matching `-amountPaise` and `+amountPaise` `transactions` rows in one DB transaction | `TC-09` | SQLite does not enforce cross-row sum = 0 via a single table `CHECK` constraint |
| `INV-05` | Privileged operations require authenticated and authorized users | Staff/Admin/Customer endpoints reject unauthenticated or wrong-role callers | Service / Security (`S`) | Explicit role checks inside every protected service method via `SecurityContext` | `TC-04` | Local desktop process memory is trusted once user authenticates |
| `INV-06` | Customers cannot access other customers' accounts | Customers can view/transfer only from the account linked to their own `user_id` | Service (`S`) | Service resolves account by authenticated session `userId` (`CustomerBankingService`), never arbitrary client `accountId` | `TC-04` | Customer can still specify a recipient account number as transfer destination |
| `INV-07` | Frozen accounts cannot participate in new financial operations under v1 policy | `FROZEN` accounts reject deposits, withdrawals, incoming transfers, and outgoing transfers | Service (`S`) + DB (`P`) | Service status guard + SQL conditional `WHERE status = 'ACTIVE'` on balance update | `TC-15` | Freeze/unfreeze maker-checker workflow is scoped as P1 |
| `INV-08` | Committed financial operations have traceable journal entries | Every committed operation has 1 `operations` row and >= 1 `transactions` row | Service (`S`) + DB (`P`) | Service inserts `operations` and `transactions` inside the same `TransactionExecutor` block as balance update | `TC-05`, `TC-06`, `TC-09`, `TC-13` | Relies on services always creating operations through approved paths |
| `INV-09` | Successful journal entries cannot be edited or deleted through normal application workflows | `operations` and `transactions` are append-only | DAO (`P`) + DB (`P`) | No `update`/`delete` methods exposed on `OperationDao` or `TransactionDao`; `ON DELETE RESTRICT` FKs | `TC-13`, `TC-14` | SQLite file on disk lacks OS-independent immutability against external tools |
| `INV-10` | Sensitive staff/admin changes have actor-linked audit records | Initial admin setup, staff provisioning/deactivation, and freeze decisions log actor-linked events | Service (`S`) + DB (`P`) | `AuditEventDao.insert(...)` invoked inside the mutating service transaction (`actor_user_id` FK) | `TC-02`, `TC-03`, `TC-15` | Rolled-back/rejected attempts are logged via `java.util.logging`, not rolled-back DB rows (`RISK-06`) |
| `INV-11` | Reconciliation never silently alters stored financial records | Discrepancies between `balance_paise` and `SUM(delta_paise)` are reported only | ReconciliationService (`S`, P1) | Read-only transaction / query compares stored vs. expected sum and returns mismatch report | `TC-16` | Requires human investigation to diagnose root cause |
| `INV-12` | Monetary arithmetic must prevent overflow | Paise addition/subtraction cannot silently wrap around `Long.MAX_VALUE` | `Money` / Service (`S`) + DB (`P`) | `Math.addExact` / `Math.subtractExact` and bounded `Money` validation before SQL write | `TC-08` | SQLite `INTEGER` supports 64-bit signed values matching Java `long` |
