# Team Handoff — Member P: Database, JDBC & Reliability (`02_P_DATABASE_AND_JDBC.md`)

> **Peer Role:** Equal Module Owner (**P**) — No hierarchical team lead.
> **Scope:** SQLite Database Model, Plain JDBC Architecture, DAO Interfaces & Implementations, `TransactionExecutor`, Schema Initialization, Constraints & Persistence Testing.
> **Current Status:** `NOT STARTED` (M1 Architecture & Contract Specification Under Review)

---

## 1. Module Purpose

The **Database, JDBC & Reliability** module delivers durable, constraint-enforced SQLite persistence over **Plain JDBC** and the **DAO pattern**. It guarantees that multi-DAO financial operations share a single JDBC `Connection` via `TransactionExecutor`, enforce foreign keys and non-negative balance checks in SQL, and roll back cleanly on failure.

---

## 2. Responsibilities & M1 / M2 Deliverables

### M1 Deliverables (Current Milestone — Documentation Only)
- Specify the 5-table relational schema (`users`, `accounts`, `operations`, `transactions`, `audit_events`) plus optional P1 extensions (`account_action_requests`, `operations.idempotency_key`) in [../architecture/DATABASE_DESIGN.md](../architecture/DATABASE_DESIGN.md).
- Define connection pragmas (`PRAGMA foreign_keys = ON`, `busy_timeout`, write-lock behavior), indexes, `ON DELETE RESTRICT` rules, atomic `AC000001` generation, and ISO-8601 UTC timestamps.
- Finalize `TransactionExecutor`, `TransactionWork<T>`, `BankingUnitOfWork`, and DAO signatures with **S**.

### M2 Implementation Tasks (After M1 Sign-Off)
- Author SQLite schema initialization script under `src/main/resources/db/` (M2; not created in M1).
- Implement connection factory, per-connection `PRAGMA foreign_keys = ON` setup, schema bootstrapper, `TransactionExecutor`, and `BankingUnitOfWork` (`db/`).
- Implement DAO interfaces (`dao/`) and parameterized `PreparedStatement` JDBC implementations (`dao/jdbc/`) for `UserDao`, `AccountDao`, `OperationDao`, `TransactionDao`, and `AuditEventDao` (plus P1 `AccountActionRequestDao`).
- Implement conditional atomic balance updates (`WHERE id = ? AND status = 'ACTIVE' [AND balance_paise >= ?]`) verifying `rowsAffected == 1`.
- Author persistence, constraint, rollback, and concurrency tests under `src/test/java/com/tiet/banking/dao/`.

---

## 3. Owned Directories

| Directory | Ownership Type | Notes |
| :--- | :---: | :--- |
| `src/main/java/com/tiet/banking/db/` | **Primary Owner** | Connection factory, `PRAGMA` setup, schema initializer, `TransactionExecutor`, `BankingUnitOfWork` |
| `src/main/java/com/tiet/banking/dao/` | **Primary Owner** | Persistence interfaces (`UserDao`, `AccountDao`, `OperationDao`, `TransactionDao`, `AuditEventDao`) |
| `src/main/java/com/tiet/banking/dao/jdbc/` | **Primary Owner** | Plain JDBC implementations (`PreparedStatement`, `ResultSet` mapping) |
| `src/main/resources/db/` | **Primary Owner** | SQLite DDL script (`schema.sql` in M2) |
| `data/` | **Primary Owner** | Runtime SQLite database location (`.gitkeep` only tracked; `.db` files gitignored) |
| `src/main/java/com/tiet/banking/model/` | **Co-owned w/ S** | Entity alignment for clean JDBC mapping |
| `src/test/java/com/tiet/banking/dao/` | **Primary Owner** | Automated JDBC DAO, constraint, rollback & concurrency tests |

---

## 4. Dependencies on Other Modules

- **Coordinates w/ S (`model/`, `exception/`, `service/`):** Needs agreed domain entities (`User`, `BankAccount`, `BankOperation`, `BankTransaction`, `AuditEvent`), persistence exception wrapper, and exact `TransactionExecutor` / `BankingUnitOfWork` callback contract.

---

## 5. Expected Interfaces (Frozen M1 Contracts — Awaiting PR #1 Approval)

- **Exposed to S:** `TransactionExecutor` (throwing `CommitOutcomeUncertainException` on commit failure), `TransactionWork<T>`, `BankingUnitOfWork`, and the 5 core DAOs (plus P1 `AccountActionRequestDao`) specified in [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md).
- **Consumed from S:** Domain models (`model/`), wrapped persistence exceptions (`exception/`), and integer-paise monetary conventions (`ADR-003`).

---

## 6. Integration Boundaries

- **Strict Layer Isolation:** `db/`, `dao/`, and `dao.jdbc/` must **never** import `service.*`, `ui.*`, or `javafx.*`.
- **Single Shared Connection:** All DAOs invoked via `BankingUnitOfWork` inside `TransactionExecutor` must operate on the **exact same JDBC `Connection`**.
- **No Unapproved Mutations on Journal Tables:** `OperationDao`, `TransactionDao`, and `AuditEventDao` must expose insert/query operations only—no update or delete methods (`INV-09`).
- **Simple, Clean JDBC:** Keep SQL queries clear and concise without custom ORM reflection or unnecessary builder wrappers ([../../AGENTS.md](../../AGENTS.md#5-student-level-engineering-code-quality--practical-collaboration)).

---

## 7. Review Responsibilities

- Cross-review **S**'s PRs on `model/` and `TransactionExecutor` service consumption to confirm financial operations verify `rowsAffected == 1` and avoid unsafe retries.
- Verify no SQLite runtime `.db` / `-wal` / `-shm` files ever appear in commits.

---

## 8. Acceptance Criteria

- Fresh SQLite DB initialization cleanly applies all 5 tables, `UNIQUE`, `FOREIGN KEY (ON DELETE RESTRICT)`, `CHECK` constraints, and indexes with `PRAGMA foreign_keys = ON`.
- Conditional debits/credits reject negative balances (`INV-01`) and `FROZEN` accounts (`INV-07`) at SQL boundary.
- Mid-transaction failure leaves zero partial writes after rollback (`TC-10`, `TC-11`).
- Concurrent withdrawals safely serialize without producing negative balances (`TC-12`), and data persists across application restarts (`TC-14`).

---

## 9. Testing Responsibilities

- **DAO & Constraint Tests (`test/.../dao/`):** Verify schema creation, foreign key restriction (`ON DELETE RESTRICT`), `CHECK` constraints, `PreparedStatement` queries, rollback on exception, and two-thread withdrawal safety against an isolated temporary SQLite file.
- **Joint Integration Tests:** Support **S** on end-to-end integration tests under `test/.../integration/`.

---

## 10. What Is Explicitly Outside This Module's Ownership

- Authentication/hashing, RBAC enforcement, `Money` input parsing, or business workflow rules (owned by **S**).
- JavaFX screens, controllers, CSS styling, or user feedback dialogs (owned by **R**).
- Introducing Hibernate, JPA, Spring Data, or MyBatis.

---

## 11. Current Status

- **Status:** `NOT STARTED`
- **Artifacts Existing:** Directory `.gitkeep` placeholders only; M1 schema & transaction specifications drafted on branch `docs/m1-architecture-contracts`.
- **Schema & Tests:** `0` SQL files, `0` DAO classes, `0` executed tests.

---

## 12. Resume Instructions

1. Read [../../AGENTS.md](../../AGENTS.md), [../../context/CURRENT_STATUS.md](../../context/CURRENT_STATUS.md), and [../../context/DECISIONS.md](../../context/DECISIONS.md).
2. Review [../architecture/DATABASE_DESIGN.md](../architecture/DATABASE_DESIGN.md) and [../architecture/TRANSACTION_MODEL.md](../architecture/TRANSACTION_MODEL.md) with **S** (`AC000001` atomic assignment and `TransactionExecutor` signatures are finalized in PR #1).
3. Once M1 PR #1 is approved and merged, open branch `feature/database-jdbc` for M2 schema and DAO implementation.
