# Team Handoff — Member P: Database, JDBC & Reliability (`02_P_DATABASE_AND_JDBC.md`)

> **Peer Role:** Equal Module Owner (**P**) — No hierarchical team lead.
> **Scope:** SQLite Schema, JDBC Connectivity, Transaction Infrastructure, DAO Interfaces & Implementations, Constraints & Persistence Testing.
> **Current Status:** `NOT STARTED` (M0 Documentation & Scaffolding Phase)

---

## 1. Module Purpose

The **Database, JDBC & Reliability** module guarantees durable, constraint-enforced relational persistence in SQLite using **Plain JDBC** and the **DAO design pattern**. It abstracts all SQL mechanics away from business logic while guaranteeing foreign-key integrity, SQL injection protection, and reliable transaction rollback behavior.

---

## 2. Responsibilities

- Design and maintain the SQLite relational schema and initialization scripts (`src/main/resources/db/`).
- Implement SQLite connection management, required PRAGMA execution (`foreign_keys = ON`, proposed `WAL` mode and `busy_timeout`), and clean resource closing (`db/`).
- Provide the transaction boundary helper/coordinator (`ADR-004`, `RISK-01`) enabling **S** to run multi-DAO operations atomically (`setAutoCommit(false)`, `commit`, `rollback`).
- Define DAO interfaces (`dao/`) and plain JDBC implementations (`dao/jdbc/`) using parameterized `PreparedStatement`s exclusively.
- Enforce relational integrity (`PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE`, `CHECK` constraints for non-negative balances and positive transaction amounts).
- Map SQL exceptions cleanly to **S**'s persistence exception types without leaking raw SQL internals to the UI.
- Co-author domain model alignment (`model/`) with **S** to ensure clean mapping between Java objects (`BigDecimal`) and SQLite storage (`paise` minor units or agreed representation).

---

## 3. Owned Directories

| Directory | Ownership Type | Notes |
| :--- | :---: | :--- |
| `src/main/java/com/tiet/banking/db/` | **Primary Owner** | Connection factory, PRAGMA setup, schema initializer, transaction helper |
| `src/main/java/com/tiet/banking/dao/` | **Primary Owner** | Persistence interfaces (`UserDao`, `CustomerDao`, `AccountDao`, etc.) |
| `src/main/java/com/tiet/banking/dao/jdbc/` | **Primary Owner** | Plain JDBC implementations (`PreparedStatement`, `ResultSet` mapping) |
| `src/main/resources/db/` | **Primary Owner** | SQL schema DDL and optional synthetic seed data script (M2) |
| `data/` | **Primary Owner** | Runtime SQLite database location (`.gitkeep` only tracked; `.db` files gitignored) |
| `src/main/java/com/tiet/banking/model/` | **Co-owned w/ S** | Entity alignment for clean JDBC mapping |
| `src/test/java/com/tiet/banking/dao/` | **Primary Owner** | Automated JDBC DAO, constraint & rollback tests |

---

## 4. Dependencies on Other Modules

- **Depends on S (`model/`, `exception/`):** Needs finalized domain model entities/enums and wrapped persistence exception classes before DAO implementations can compile.
- **Collaborates with S on Transaction Semantics (`RISK-01`):** Must agree with **S** on how an active JDBC `Connection` is shared across multiple DAO calls inside a single business transaction.

---

## 5. Expected Interfaces (Provisional — Pending M1 Freeze)

- **Exposed to S:**
  - Transaction Coordinator abstraction (executes a unit of work atomically in `db/`).
  - DAO interfaces (`UserDao`, `CustomerDao`, `AccountDao`, `TransactionDao`, and P1 `FreezeRequestDao`, `AuditLogDao`) documented conceptually in [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md).
- **Consumed from S:** Domain models (`model/`), persistence wrapper exceptions (`exception/`), and approved money conversion conventions (`ADR-003`).

---

## 6. Integration Boundaries

- **Strict Layer Isolation:** `dao.*`, `dao.jdbc.*`, and `db.*` must **never** import `com.tiet.banking.ui.*` or `javafx.*`, and must **never** import `com.tiet.banking.service.*` business rules.
- **No Business Authorization in SQL DAOs:** Role permissions and customer ownership validation live in **S**'s service layer (`ADR-005`); DAOs execute explicit queries requested by services.
- **Zero SQL Concatenation:** Every dynamic parameter must bind through `PreparedStatement` placeholders (`?`).
- **Git Hygiene:** Runtime `.db`, `.db-wal`, `.db-shm`, and `.db-journal` files under `data/` must never be committed.

---

## 7. Initial Tasks (For M1 -> M2 Transition)

1. Co-review `ADR-003` (integer `paise` vs. decimal text in SQLite) and `ADR-004` / `RISK-01` (transaction manager design) with **S**.
2. Finalize relational table design, foreign keys, `CHECK` constraints, and ledger immutability rules in [../architecture/DATABASE_DESIGN.md](../architecture/DATABASE_DESIGN.md).
3. On branch `feature/database-jdbc` (in M2), write schema SQL (`resources/db/`), `db/` connection/transaction helpers, JDBC DAOs, and DAO test suite.

---

## 8. Acceptance Criteria

- Fresh SQLite initialization cleanly creates all tables, constraints, and indexes with `PRAGMA foreign_keys = ON`.
- Database rejects negative balances, non-positive transaction amounts, duplicate usernames/account numbers, and invalid foreign keys at the SQL constraint level.
- Multi-DAO transfer failure mid-flight cleanly rolls back all partial changes (`TC-DB-04`).
- No floating-point (`REAL`) columns store money (`ADR-003`).
- All DAO methods execute parameterized `PreparedStatement`s and close JDBC resources via `try-with-resources`.

---

## 9. Testing Responsibilities

- **DAO & Constraint Tests (`src/test/java/com/tiet/banking/dao/`):** Test CRUD queries, constraint enforcement (`CHECK`, `FOREIGN KEY`, `UNIQUE`), connection PRAGMAs, and atomic commit/rollback against an isolated temporary SQLite test file (`TC-DB-01`..`TC-DB-05` in [../testing/TEST_PLAN.md](../testing/TEST_PLAN.md)).
- **Integration Support:** Partner with **S** on end-to-end integration tests under `src/test/java/com/tiet/banking/integration/`.

---

## 10. What Is Explicitly Outside This Module's Ownership

- Business validation logic (e.g., RBAC checks, password hashing, maker-checker policy enforcement — owned by **S**).
- JavaFX layouts, controllers, dialogs, CSS styling, and UI navigation (owned by **R**).
- Introducing Hibernate, JPA, Spring Data, or MyBatis (explicitly excluded by UCS553 scope).

---

## 11. Current Status

- **Status:** `NOT STARTED`
- **Artifacts Existing:** Directory `.gitkeep` placeholders only (`db/`, `dao/`, `dao/jdbc/`, `resources/db/`, `data/`, `test/.../dao/`).
- **Schema & Tests:** `0` SQL files, `0` DAO classes, `0` executed tests.

---

## 12. Resume Instructions

1. Read [../../AGENTS.md](../../AGENTS.md), [../../context/00_MASTER_PROJECT_CONTEXT.md](../../context/00_MASTER_PROJECT_CONTEXT.md), and [../../context/CURRENT_STATUS.md](../../context/CURRENT_STATUS.md).
2. Review [../architecture/DATABASE_DESIGN.md](../architecture/DATABASE_DESIGN.md), [../architecture/TRANSACTION_MODEL.md](../architecture/TRANSACTION_MODEL.md), and [../../context/KNOWN_ISSUES.md](../../context/KNOWN_ISSUES.md) (`RISK-01`, `RISK-03`, `RISK-06`).
3. Align with **S** on `ADR-003` and `ADR-004` during M1 before creating schema or JDBC code on branch `feature/database-jdbc`.
