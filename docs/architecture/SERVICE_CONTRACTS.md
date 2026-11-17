# Service, Domain Model, DTO & DAO Contracts (`SERVICE_CONTRACTS.md`)

> **Version:** M1 v2.0 Specification (`PROPOSED — AWAITING TEAM REVIEW`)
> **Status:** Conceptual & Descriptive Signatures Only (No Executable Java Code Implemented)
> **Notice:** Keep DTOs and service interfaces concise and student-maintainable; flag conflicts rather than silently inventing unapproved compromises.

---

## 1. Proposed Domain Models & Value Types (`com.tiet.banking.model`)

Co-owned by **S** and **P** for clean service-to-JDBC mapping.

### 1.1 Enumerations
- **`Role`**: `CUSTOMER`, `STAFF`, `ADMIN`
- **`AccountStatus`**: `ACTIVE`, `FROZEN` *(v1 scope; closure deferred beyond P0)*
- **`OperationType`**: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`
- **`AccountActionType`** *(P1)*: `FREEZE`, `UNFREEZE`
- **`RequestStatus`** *(P1)*: `PENDING`, `APPROVED`, `REJECTED`

### 1.2 Value Object: `Money`
- **Fields:** `long paise` (immutable minor units; `100 paise = ₹1.00`)
- **Responsibilities:**
  - `Money.parsePositive(String rawInput)` / `Money.fromBigDecimal(BigDecimal amount)`: rejects `null`, blank, `<= 0`, scale `> 2`, and `long` overflow (`INV-02`, `INV-12`).
  - `Money.ofPaise(long paise)`: wraps persisted `long` values (non-negative for balances; signed for deltas where appropriate).
  - `Money.add(Money)` / `Money.subtract(Money)`: uses `Math.addExact` / `Math.subtractExact`.
  - `toBigDecimal()` / `toFormattedString()`: formats exact 2-decimal currency display without floating-point conversion.

### 1.3 Core Domain Entities
| Entity | Fields & Conceptual Types | Ownership / Notes |
| :--- | :--- | :--- |
| **`User`** | `long id`, `String fullName`, `String email`, `String passwordHash`, `String passwordSalt`, `String passwordAlgo`, `Role role`, `boolean active`, `Instant createdAt` | Core principal; `CUSTOMER` profiles merged here (M1 simplification) |
| **`BankAccount`** | `long id`, `long userId`, `String accountNumber`, `long balancePaise`, `AccountStatus status`, `Instant createdAt` | 1:1 with `CUSTOMER` `User` in v1 (`balancePaise >= 0`) |
| **`BankOperation`** | `long id`, `String referenceId`, `OperationType operationType`, `long performedByUserId`, `String idempotencyKey` *(nullable, P1)*, `Instant createdAt` | Parent operation record linking journal entries |
| **`BankTransaction`** | `long id`, `long operationId`, `long accountId`, `long deltaPaise`, `long balanceAfterPaise`, `Instant createdAt` | Immutable signed account journal entry (`deltaPaise != 0`) |
| **`AuditEvent`** | `long id`, `long actorUserId`, `String action`, `String targetType`, `String targetId`, `Instant createdAt` | Actor-linked security/admin audit record |
| **`AccountActionRequest`** *(P1)* | `long id`, `long accountId`, `AccountActionType actionType`, `String reason`, `long requestedByUserId`, `RequestStatus status`, `Long reviewedByUserId`, `Instant createdAt`, `Instant reviewedAt` | Optional maker-checker freeze/unfreeze entity |

---

## 2. Presentation-Facing DTOs (`com.tiet.banking.dto`)

Co-owned by **S** and **R**. JavaFX controllers consume these DTOs exclusively and never depend on DAO entities or `java.sql.*`.

| DTO | Fields & Types | Purpose & Ownership |
| :--- | :--- | :--- |
| **`UserSession`** *(or `AuthenticatedSession`)* | `long userId`, `String fullName`, `String email`, `Role role`, `Long accountId` *(nullable for Staff/Admin)*, `String accountNumber` *(nullable)* | Returned on login and held in active session context (`S` -> `R`) |
| **`AccountSummary`** | `long accountId`, `String accountNumber`, `long ownerUserId`, `String ownerName`, `String ownerEmail`, `long balancePaise`, `String formattedBalance`, `AccountStatus status`, `Instant createdAt` | Customer dashboard & Staff customer inspection card (`S` -> `R`) |
| **`TransactionReceipt`** | `String referenceId`, `OperationType operationType`, `String accountNumber`, `String counterpartyAccountNumber` *(nullable)*, `long amountPaise`, `String formattedAmount`, `long balanceAfterPaise`, `String formattedBalanceAfter`, `Instant timestamp` | Immediate confirmation shown after deposit, withdrawal, or transfer |
| **`TransactionView`** | `String referenceId`, `OperationType operationType`, `long deltaPaise`, `String formattedDelta`, `long balanceAfterPaise`, `String formattedBalanceAfter`, `String performedByRole`, `Instant timestamp` | Row model for JavaFX `TableView` transaction history |
| **`CustomerSearchResult`** | `long userId`, `String fullName`, `String email`, `boolean userActive`, `String accountNumber`, `AccountStatus accountStatus`, `long balancePaise`, `String formattedBalance` | Row model for Staff customer/account search table |
| **`AdministrativeSummary`** | `long totalCustomers`, `long totalActiveStaff`, `long totalAccounts`, `long frozenAccounts`, `long totalOperationsCount`, `long totalSystemBalancePaise`, `String formattedTotalBalance` | High-level read-only metrics for Admin dashboard |
| **`ReconciliationResult`** *(P1)* | `long accountId`, `String accountNumber`, `long storedBalancePaise`, `long expectedBalancePaise`, `long discrepancyPaise`, `boolean matched` | Per-account balance vs. ledger verification outcome |
| **`AuditEventView`** *(P1)* | `long auditId`, `long actorUserId`, `String actorEmail`, `Role actorRole`, `String action`, `String targetType`, `String targetId`, `Instant timestamp` | Row model for Admin Audit Viewer |
| **`AccountActionRequestView`** *(P1)* | `long requestId`, `String accountNumber`, `String ownerName`, `AccountActionType actionType`, `String reason`, `String requestedByEmail`, `RequestStatus status`, `String reviewedByEmail`, `Instant createdAt`, `Instant reviewedAt` | Row model for P1 freeze/unfreeze queue |

---

## 3. Proposed Business Service Contracts (`com.tiet.banking.service`)

> **Session & Exception Conventions:** Unless marked `UNAUTHENTICATED`, every service method validates the active `UserSession` in `SecurityContext` (`S`) before performing work. Custom exception concepts: `ValidationException`, `AuthenticationException`, `AuthorizationException`, `ResourceNotFoundException`, `InsufficientFundsException`, `AccountFrozenException`, `ConflictException` (duplicate email/idempotency/bootstrap closed), and `PersistenceException`.

### 3.1 `AuthService`

| Method Name | Caller Role | Inputs | Return DTO | Authorization & Preconditions | Expected Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `isInitialAdminSetupRequired()` | `UNAUTHENTICATED` | None | `boolean` | Public check at startup; `true` iff zero `ADMIN` users exist | `PersistenceException` | Read-only single query | Determines whether JavaFX shows first-run Admin setup screen |
| `setupInitialAdmin(...)` | `UNAUTHENTICATED` | `fullName: String`, `email: String`, `password: char[]` | `UserSession` | Allowed **only** when zero `ADMIN` users exist; valid non-blank name, email format, password policy | `ConflictException` (if admin already exists), `ValidationException`, `PersistenceException` | Single atomic DB transaction: insert `ADMIN` user + insert `audit_events` (`INITIAL_ADMIN_CREATED`) | First `ADMIN` persisted with salted hash, bootstrap permanently disabled, admin session started |
| `registerCustomer(...)` | `UNAUTHENTICATED` | `fullName: String`, `email: String`, `password: char[]` | `UserSession` | Public registration; role hardcoded to `CUSTOMER` (never caller-supplied); unique email | `ValidationException`, `ConflictException` (duplicate email), `PersistenceException` | Single atomic DB transaction: insert `CUSTOMER` user + insert `accounts` (`balance_paise=0`, `status=ACTIVE`) | Customer and zero-balance account created atomically; customer logged in |
| `login(...)` | `UNAUTHENTICATED` | `email: String`, `password: char[]` | `UserSession` | Non-blank email/password; user exists; `is_active == true`; hash matches | `ValidationException`, `AuthenticationException` | Read-only query (+ `UserDao`/`AccountDao` lookup) | Active `UserSession` established for `CUSTOMER`, `STAFF`, or `ADMIN` |
| `logout()` | Any authenticated | None | `void` | Clears current `SecurityContext` session | None | None | Active session cleared; UI returns to login screen |
| `getCurrentSession()` | Any | None | `Optional<UserSession>` | Returns active session if logged in | None | None | Used by UI navigation & guards |

### 3.2 `CustomerBankingService`

| Method Name | Caller Role | Inputs | Return DTO | Authorization & Preconditions | Expected Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `getOwnAccountSummary()` | `CUSTOMER` | None *(uses session `userId`)* | `AccountSummary` | Must be `CUSTOMER`; queries strictly by authenticated `userId` (`INV-06`) | `AuthorizationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only query | Returns customer's own balance, account number, and `ACTIVE`/`FROZEN` status |
| `getOwnTransactionHistory(...)` | `CUSTOMER` | `limit: int` *(optional/bounded)* | `List<TransactionView>` | Must be `CUSTOMER`; queries strictly own `accountId` | `AuthorizationException`, `ValidationException`, `PersistenceException` | Read-only query | Chronological (newest-first) journal rows for customer's own account |
| `transferFunds(...)` | `CUSTOMER` | `destinationAccountNumber: String`, `amountInput: String` *(or `BigDecimal`)*, `idempotencyKey: String` *(nullable; P1)* | `TransactionReceipt` | Must be `CUSTOMER`; source account resolved from session `userId`; source `!=` destination; valid positive `Money`; both accounts `ACTIVE`; sufficient source balance | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `AccountFrozenException`, `InsufficientFundsException`, `ConflictException` (P1 idempotency), `PersistenceException` | **Mandatory atomic transaction** (`TransactionExecutor`): verify states, conditional debit + credit (`rowsAffected==1`), insert `1` `TRANSFER` operation + `2` `transactions` rows summing to `0` | Source debited, destination credited atomically; receipt returned |
| `exportOwnStatementCsv(...)` *(P1)* | `CUSTOMER` | `targetFile: Path` | `Path` | Must be `CUSTOMER`; valid writable path; exports strictly own account history | `AuthorizationException`, `ValidationException`, `PersistenceException` | Read-only query + Java I/O CSV write | Well-formed CSV statement saved to local disk |

### 3.3 `TellerService`

| Method Name | Caller Role | Inputs | Return DTO | Authorization & Preconditions | Expected Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `depositCash(...)` | `STAFF` | `accountNumber: String`, `amountInput: String`, `idempotencyKey: String` *(nullable; P1)* | `TransactionReceipt` | Must be `STAFF` (Admin denied per `ROLE_PERMISSIONS`); active staff user; valid positive `Money`; target account exists and is `ACTIVE` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `AccountFrozenException`, `PersistenceException` | **Mandatory atomic transaction**: conditional credit (`WHERE status='ACTIVE'`), insert `DEPOSIT` operation + `1` positive `transactions` row | Target account balance increased; receipt returned |
| `withdrawCash(...)` | `STAFF` | `accountNumber: String`, `amountInput: String`, `idempotencyKey: String` *(nullable; P1)* | `TransactionReceipt` | Must be `STAFF` (Admin denied); active staff user; valid positive `Money`; target account exists, `ACTIVE`, and `balance >= amount` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `AccountFrozenException`, `InsufficientFundsException`, `PersistenceException` | **Mandatory atomic transaction**: conditional debit (`WHERE status='ACTIVE' AND balance_paise >= ?`), insert `WITHDRAWAL` operation + `1` negative `transactions` row | Target account balance decreased; receipt returned |

### 3.4 `StaffService`

| Method Name | Caller Role | Inputs | Return DTO | Authorization & Preconditions | Expected Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `searchCustomers(...)` | `STAFF` | `query: String` *(name, email, or account number)* | `List<CustomerSearchResult>` | Must be `STAFF`; non-null bounded query (`""` may list recent/all synthetic customers capped reasonably) | `AuthorizationException`, `ValidationException`, `PersistenceException` | Read-only parameterized `LIKE` query (`role = 'CUSTOMER'`) | Returns matching customer account summaries for teller lookup |
| `getCustomerAccountSummary(...)` | `STAFF` | `accountNumber: String` | `AccountSummary` | Must be `STAFF`; valid non-blank account number | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only query | Displays authorized customer account details |
| `getCustomerTransactionHistory(...)` | `STAFF` | `accountNumber: String`, `limit: int` | `List<TransactionView>` | Must be `STAFF`; account exists | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only query | Displays chronological transactions for requested customer account |

### 3.5 `AdminService`

| Method Name | Caller Role | Inputs | Return DTO | Authorization & Preconditions | Expected Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `createStaffUser(...)` | `ADMIN` | `fullName: String`, `email: String`, `password: char[]` | `User` *(safe summary without hash/salt or dedicated summary)* | Must be `ADMIN`; valid fields; unique email | `AuthorizationException`, `ValidationException`, `ConflictException`, `PersistenceException` | **Atomic transaction**: insert `STAFF` user + insert `audit_events` (`STAFF_CREATED`) | Staff login provisioned and audited |
| `setStaffActiveStatus(...)` | `ADMIN` | `staffUserId: long`, `active: boolean` | `void` | Must be `ADMIN`; target user must exist and have `role == STAFF` (prevents deactivating self/admin via this endpoint) | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `PersistenceException` | **Atomic transaction**: update `users.is_active` + insert `audit_events` (`STAFF_ACTIVATED` / `STAFF_DEACTIVATED`) | Staff login enabled/disabled and audited |
| `listStaffUsers()` | `ADMIN` | None | `List<User>` *(without sensitive hash fields exposed to UI)* | Must be `ADMIN` | `AuthorizationException`, `PersistenceException` | Read-only query (`WHERE role = 'STAFF'`) | Displays provisioned staff directory in Admin UI |
| `getAdministrativeSummary()` | `ADMIN` | None | `AdministrativeSummary` | Must be `ADMIN` | `AuthorizationException`, `PersistenceException` | Read-only aggregate query | Displays system counts and simulated total balance |
| `getAuditEvents(...)` *(P1 UI)* | `ADMIN` | `limit: int`, `actionFilter: String` *(nullable)* | `List<AuditEventView>` | Must be `ADMIN` | `AuthorizationException`, `ValidationException`, `PersistenceException` | Read-only query (`audit_events JOIN users`) | Populates Admin Audit Viewer table |

### 3.6 `ReconciliationService` *(P1)*

| Method Name | Caller Role | Inputs | Return DTO | Authorization & Preconditions | Expected Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `reconcileAccount(...)` | `ADMIN` | `accountNumber: String` | `ReconciliationResult` | Must be `ADMIN`; account exists | `AuthorizationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only consistent transaction (`stored` vs `SUM(delta_paise)`) | Reports match or mismatch; **never** modifies stored balances (`INV-11`) |
| `reconcileAllAccounts()` | `ADMIN` | None | `List<ReconciliationResult>` | Must be `ADMIN` | `AuthorizationException`, `PersistenceException` | Read-only consistent transaction across all accounts | Returns full reconciliation report for Admin inspection |

### 3.7 `AccountControlService` *(P1 — Maker-Checker)*

| Method Name | Caller Role | Inputs | Return DTO | Authorization & Preconditions | Expected Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `requestAccountFreezeChange(...)` | `STAFF` | `accountNumber: String`, `actionType: AccountActionType`, `reason: String` | `AccountActionRequestView` | Must be `STAFF`; non-empty reason; account exists; no duplicate `PENDING` request; target state differs from current state | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `ConflictException`, `PersistenceException` | **Atomic transaction**: insert `account_action_requests` (`PENDING`) + insert `audit_events` | Request queued for Admin review |
| `listPendingRequests()` | `ADMIN` *(or `STAFF` read-only)* | None | `List<AccountActionRequestView>` | Must be `ADMIN` or `STAFF` | `AuthorizationException`, `PersistenceException` | Read-only query | Lists pending freeze/unfreeze queue |
| `reviewAccountFreezeChange(...)` | `ADMIN` | `requestId: long`, `approve: boolean` | `AccountActionRequestView` | Must be `ADMIN`; request exists and is `PENDING`; `reviewerUserId != requestedByUserId` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `ConflictException`, `PersistenceException` | **Atomic transaction**: update request status; if approved, update `accounts.status`; insert `audit_events` | Status updated atomically if approved; audit recorded |

---

## 4. Proposed DAO & Transaction Coordinator Interfaces (`com.tiet.banking.dao`, `com.tiet.banking.db`)

Owned by **P** and invoked exclusively by **S**'s services:

- **`TransactionExecutor`** (`db/`): `T execute(TransactionWork<T> work)` — opens/manages one SQLite `Connection` (`PRAGMA foreign_keys = ON`, `autoCommit = false`), commits on success, rolls back on exception.
- **`TransactionWork<T>`** (`db/`): functional callback `T execute(BankingUnitOfWork uow)`.
- **`BankingUnitOfWork`** (`db/`): provides transaction-scoped DAO instances (`users()`, `accounts()`, `operations()`, `transactions()`, `auditEvents()`, and P1 `accountActionRequests()`) bound to the single active JDBC `Connection`.
- **Core DAOs (`dao/`):**
  - `UserDao`: `insert`, `findById`, `findByEmail`, `countByRole`, `findByRole`, `searchCustomers`, `updateActiveStatus`
  - `AccountDao`: `insert`, `findById`, `findByUserId`, `findByAccountNumber`, `creditIfActive`, `debitIfActiveAndSufficient`, `updateStatus` *(P1)*, `getAggregateSummary`
  - `OperationDao`: `insert`, `findByReferenceId`, `findByIdempotencyKey` *(P1)*
  - `TransactionDao`: `insert`, `findByAccountIdOrdered`, `sumDeltasByAccountId` *(P1)*
  - `AuditEventDao`: `insert`, `findRecentFiltered` *(P1 UI)*
  - `AccountActionRequestDao` *(P1)*: `insert`, `findPending`, `findById`, `updateDecisionIfPending`

---

## 5. Open M1 Contract Review Items (Explicitly Flagged)

1. **Idempotency Parameter in P0 Signatures:** `transferFunds`, `depositCash`, and `withdrawCash` show `idempotencyKey` as nullable/reserved for P1 (`operations.idempotency_key`). Confirm during M1 review whether P0 method signatures omit it until P1 or accept nullable `String`.
2. **Account Number Format:** Finalize exact synthetic account number generation format (e.g., `TIET` + 8-digit sequence vs. 10-digit numeric string) between `S` and `P`.
3. **Mock/Stub Strategy for R:** Once these DTOs and Service interfaces are signed off in M1, **R** can build all JavaFX screens against temporary in-memory mock service implementations before **S** and **P** complete database wiring.
