# Technical Context (`TECHNICAL_CONTEXT.md`)

> **Focus:** Proposed Technical Architecture, Layer Responsibilities, Money Handling, Transaction Integrity, Security Principles, and Build Conventions.
> **Notice:** All technical designs below are **PROVISIONAL PROPOSALS**. No Java classes, method signatures, SQL schemas, or `pom.xml` exist yet.

---

## 1. Layer Stack Overview

For full architectural descriptions and diagrams, refer to [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md) and [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md).

1. **Presentation Layer (`com.tiet.banking.ui.*` — Owner: R)**
   - Built with JavaFX (views/controllers/CSS under `src/main/resources/styles/`).
   - Organized by domain area: `ui/common` (shell, navigation, alert dialogs), `ui/auth` (login/registration), `ui/customer`, `ui/staff`, `ui/admin`.
   - Interacts **only** with Business Service contracts using DTOs and domain enums. Never imports `dao.*`, `db.*`, or `java.sql.*`.

2. **Business Service & Security Layer (`com.tiet.banking.service.*`, `security.*`, `dto.*`, `exception.*` — Owner: S)**
   - Encapsulates all banking business rules, input validation, role checks, customer-account ownership verification, and financial workflow orchestration.
   - Coordinates multi-step persistence calls inside explicit transaction boundaries.
   - Returns structured DTOs / domain models and throws domain-meaningful custom exceptions.

3. **DAO & Persistence Layer (`com.tiet.banking.dao.*`, `dao.jdbc.*`, `db.*` — Owner: P)**
   - Plain JDBC over SQLite (`data/` directory at runtime; gitignored).
   - `db/` manages connection acquisition, SQLite PRAGMA initialization (`PRAGMA foreign_keys = ON`), schema bootstrap, and transaction context support.
   - `dao/` defines persistence interfaces; `dao/jdbc/` implements them using `PreparedStatement` and `ResultSet` mapping without business logic.

4. **Domain Models & Cross-Cutting Packages**
   - `model/`: Core entities (Customer, Account, Transaction/LedgerEntry, UserAccount, FreezeRequest, AuditEntry) and status enums.
   - `util/`: Input validators, formatting helpers, Java I/O statement export helpers, and `java.util.logging` configuration.
   - `bootstrap/`: Application assembly / manual dependency injection wiring services, DAOs, and JavaFX startup.

---

## 2. Money Handling Proposal (`ADR-003`)

- **Strict Prohibition:** `float` and `double` **must never** be used to represent, store, or calculate currency balances or transaction amounts due to binary floating-point rounding drift.
- **Proposed Java Representation:** `java.math.BigDecimal` with explicit scale (`2` decimal places for INR/paise simulation) and deterministic rounding (`RoundingMode.HALF_UP` or strict exact-scale validation).
- **Proposed SQLite Persistence (Pending M1 Decision):** Either `INTEGER` storing minor currency units (`paise`, i.e., `100` = `1.00`) or fixed-format `TEXT` mapped carefully to `BigDecimal`. Integer minor units avoid SQLite `REAL` affinity issues and support exact `CHECK` constraints and integer `SUM()` reconciliation.

---

## 3. Database Transaction Principles (`ADR-004`)

- Financial operations (deposit, withdrawal, account-to-account transfer) must be **atomic**.
- `Connection.setAutoCommit(false)` must govern multi-step operations:
  - Example (Transfer): Validate accounts & active status -> verify sufficient funds -> debit source -> credit destination -> insert immutable transaction/ledger entry -> insert audit entry -> `commit()`.
  - Any validation failure or `SQLException` triggers `rollback()` and preserves prior balances.
- For detailed transaction rules, see [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md) and [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md).

---

## 4. Authentication & Authorization Principles (`ADR-005`)

- **Password Handling:** Passwords must never be stored in plaintext; use salted cryptographic hashing (e.g., PBKDF2 with SHA-256 via standard JDK `SecretKeyFactory` or equivalent vetted standard API) before persistence.
- **Session Context:** Authenticated principal identity (`userId`, `role`, optional `customerId`) is held in a service/security session context.
- **Defense in Depth:** JavaFX hides unauthorized screens for UX clarity, while the **Service Layer** enforces mandatory RBAC role checks and customer-account ownership checks on every operation. See [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md).

---

## 5. Exception Handling Strategy

Custom exceptions under `com.tiet.banking.exception` should cleanly separate:
- **Domain / Business Validation Exceptions** (e.g., insufficient funds, invalid transfer amount, frozen account, duplicate request).
- **Authorization / Authentication Exceptions** (e.g., invalid credentials, role unauthorized, ownership mismatch).
- **Persistence / Infrastructure Exceptions** (wrapping `SQLException` from DAOs so raw SQL details never leak into JavaFX UI messages).

---

## 6. File I/O and Logging

- **Java I/O (`util/`):** Used for exporting simulated customer account statements (e.g., formatted text or CSV reports) and loading configuration defaults safely using `try-with-resources`.
- **`java.util.logging`:** Standard JDK logger (`Logger`) recording startup events, authentication outcomes (without logging passwords/secrets), financial transaction references, and unexpected exceptions to rotating/console handlers (runtime `.log` files are gitignored).

---

## 7. Build & Packaging Conventions (Planned for M1/M2)

- **Build Tool:** Apache Maven (`pom.xml` intentionally **not created** in M0).
- **Standard Layout:** `src/main/java`, `src/main/resources`, `src/test/java` under base package `com.tiet.banking`.
- **Java Version:** Target Java 17 (`maven.compiler.source/target = 17`).
- **Expected Libraries (when `pom.xml` is introduced in M2):** JavaFX controls, `sqlite-jdbc` driver, and JUnit 5 for unit/DAO/integration testing.
