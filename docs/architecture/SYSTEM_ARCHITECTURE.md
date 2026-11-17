# System Architecture (`SYSTEM_ARCHITECTURE.md`)

> **Status:** `CONCEPTUAL PROPOSAL — NOT YET IMPLEMENTED`
> **Related Decisions:** `ADR-001`, `ADR-002`, `ADR-006` in [../../context/DECISIONS.md](../../context/DECISIONS.md)

---

## 1. Architectural Style

The **Banking Management System** uses a **strict unidirectional three-tier layered desktop architecture** implemented in Core Java 17 with JavaFX, Plain JDBC, and SQLite.

Key goals of this architecture:
- Separate JavaFX presentation logic from financial business logic and SQL queries.
- Allow **S**, **P**, and **R** to develop and verify modules independently once M1 contracts are frozen.
- Keep business rules testable via unit tests without launching JavaFX or touching production-like UI state.

---

## 2. Conceptual Layer & Dependency Diagram (Proposed — Not Yet Implemented)

```text
+=======================================================================+
|  BOOTSTRAP / WIRING (`com.tiet.banking.bootstrap`)                    |
|  Assembles DB manager, DAOs, Security, Services, and JavaFX primary   |
|  stage at startup (Owner: S with P & R integration)                   |
+=======================================================================+
           |                    |                         |
           v                    v                         v
+-------------------+  +--------------------+  +------------------------+
| PRESENTATION      |  | BUSINESS SERVICE   |  | PERSISTENCE            |
| (`ui.*` — R)      |  | (`service.*`,      |  | (`dao.*`, `dao.jdbc.*`,|
| - ui.common       |  |  `security.*` — S) |  |  `db.*` — P)           |
| - ui.auth         |->| - Auth & RBAC      |->| - Connection/Tx Mgr    |
| - ui.customer     |  | - Account & Cash   |  | - JDBC DAOs            |
| - ui.staff        |  | - Transfer Engine  |  | - PreparedStatements   |
| - ui.admin        |  | - Audit & Approval |  | - SQLite (`data/*.db`) |
+-------------------+  +--------------------+  +------------------------+
           |                    |                         |
           +--------------------+-------------------------+
                                |
                                v
+=======================================================================+
|  SHARED DOMAIN & CROSS-CUTTING PACKAGES                               |
|  - `model.*`     : Core domain entities & enums (S + P coordinated)   |
|  - `dto.*`       : Request/response view & service DTOs (S + R)       |
|  - `exception.*` : Custom banking, auth & persistence exceptions (S)  |
|  - `util.*`      : Validation, Java I/O export, java.util.logging     |
+=======================================================================+
```

---

## 3. Strict Dependency Rules

1. **`ui.*` -> `service.*`, `dto.*`, `model.*` (read-only views/enums), `exception.*`:**
   - JavaFX controllers/views render DTOs and invoke service contracts.
   - **Forbidden:** `ui.*` must **never** import `dao.*`, `dao.jdbc.*`, `db.*`, or `java.sql.*`.
2. **`service.*` & `security.*` -> `dao.*` (interfaces), `db.*` (transaction coordinator), `model.*`, `dto.*`, `exception.*`, `util.*`:**
   - Services contain business rules, authorization checks, and transaction orchestration.
   - Services depend on DAO interfaces, not `dao.jdbc.*` concrete SQL classes directly (wired via `bootstrap/`).
3. **`dao.jdbc.*` -> `dao.*`, `db.*`, `model.*`, `exception.*`:**
   - DAOs perform SQL persistence and `ResultSet`-to-model mapping only.
   - **Forbidden:** DAOs must **never** import `ui.*` or `service.*`, and must not enforce business rules like role checks or transfer policies.
4. **`model.*` & `dto.*`:**
   - Plain Java structures with validation/enums; no dependencies on UI, JDBC, or Services.

---

## 4. Package Responsibilities (Proposed)

| Package | Primary Owner | Conceptual Responsibility |
| :--- | :---: | :--- |
| `com.tiet.banking.bootstrap` | `S` (Integrated) | Application startup, configuration loading, manual dependency wiring |
| `com.tiet.banking.model` | `S` (w/ `P`) | Core domain concepts (`User`, `Customer`, `Account`, `Transaction`, `FreezeRequest`, `AuditLog`, enums) |
| `com.tiet.banking.dto` | `S` (w/ `R`) | Input/output transfer objects tailored for service operations & UI tables |
| `com.tiet.banking.service` | `S` | Authentication, customer/account management, deposits/withdrawals, transfers, governance |
| `com.tiet.banking.security` | `S` | Password hashing, session context, role & ownership guards |
| `com.tiet.banking.exception` | `S` | Domain, authorization, validation, and wrapped persistence exceptions |
| `com.tiet.banking.dao` | `P` | Persistence interfaces independent of SQLite JDBC mechanics |
| `com.tiet.banking.dao.jdbc` | `P` | Plain JDBC implementations using parameterized `PreparedStatement`s |
| `com.tiet.banking.db` | `P` | SQLite connection lifecycle, PRAGMA setup, schema initialization, transaction boundary helpers |
| `com.tiet.banking.util` | `S` (Shared) | Shared formatters, input validators, Java I/O export utilities, `java.util.logging` setup |
| `com.tiet.banking.ui.*` | `R` | JavaFX shell, auth views, role dashboards, forms, `TableView`s, CSS integration |

---

## 5. Integration Assumptions

- **Mock-Friendly UI Development:** By depending solely on service interfaces and DTOs, the JavaFX layer (`R`) can build and visually test screens using temporary in-memory mock service implementations while `S` and `P` complete persistence and business logic.
- **Single-Process Desktop Deployment:** The application runs as a local JavaFX desktop process connected to an embedded SQLite file under `data/`. Background/async UI tasks (if used for responsiveness) must respect SQLite single-writer constraints (`RISK-03`).
- **Contract-First Evolution:** Any change crossing layer boundaries (`dto/`, `service/`, `dao/`, `model/`) must be reflected in [SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md) and [DATABASE_DESIGN.md](DATABASE_DESIGN.md).
