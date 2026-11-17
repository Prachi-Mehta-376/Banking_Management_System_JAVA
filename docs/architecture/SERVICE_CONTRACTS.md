# Service, Domain Model, DTO & DAO Contracts (`SERVICE_CONTRACTS.md`)

> **Version:** M1 v2.0 Frozen Contract Specification (`RECOMMENDED — AWAITING PR #1 APPROVAL`)
> **Status:** Descriptive Specification Only (No Java `.java` Source Files Created)
> **Notice:** Signatures below freeze `S ↔ P` and `S ↔ R` boundaries using simple, syllabus-aligned Core Java types without unnecessary abstractions.

---

## 1. Domain Models & Value Types (`com.tiet.banking.model`)

Co-owned by **S** and **P**.

### 1.1 Enumerations
- **`Role`**: `CUSTOMER`, `STAFF`, `ADMIN`
- **`AccountStatus`**: `ACTIVE`, `FROZEN` *(v1 scope)*
- **`OperationType`**: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`
- **`AccountActionType`** *(P1)*: `FREEZE`, `UNFREEZE`
- **`RequestStatus`** *(P1)*: `PENDING`, `APPROVED`, `REJECTED`

### 1.2 Value Object: `Money`
- **Internal Field:** `private final long paise`
- **Key Operations:**
  - `Money.parsePositive(String input) -> Money`: trims string, parses `BigDecimal`, rejects `null`/blank/`<= 0`/`scale > 2`/overflow (`ValidationException`).
  - `Money.fromBigDecimalPositive(BigDecimal amount) -> Money`: same validation on `BigDecimal`.
  - `Money.ofPaise(long paise) -> Money`: wraps persisted `long` paise (`>= 0` variant `ofNonNegativePaise(long)`).
  - `long paise()`, `Money add(Money other)`, `Money subtract(Money other)` (using `Math.addExact` / `Math.subtractExact`).
  - `BigDecimal toBigDecimal()`, `String toFormattedString()` (2 decimal places, e.g., `"500.00"`).

### 1.3 Core Domain Entities
| Entity | Fields & Java Types | Notes |
| :--- | :--- | :--- |
| **`User`** | `long id`, `String fullName`, `String email`, `String passwordHash`, `String passwordSalt`, `String passwordAlgo`, `Role role`, `boolean active`, `Instant createdAt` | Core principal (`CUSTOMER` identity merged here) |
| **`BankAccount`** | `long id`, `long userId`, `String accountNumber`, `long balancePaise`, `AccountStatus status`, `Instant createdAt` | 1:1 with `CUSTOMER` (`accountNumber` format `AC000001`) |
| **`BankOperation`** | `long id`, `String referenceId`, `OperationType operationType`, `long performedByUserId`, `Instant createdAt` | Parent operation record (server `referenceId`; no P0 `idempotencyKey`) |
| **`BankTransaction`** | `long id`, `long operationId`, `long accountId`, `long deltaPaise`, `long balanceAfterPaise`, `Instant createdAt` | Append-only signed account journal entry |
| **`AuditEvent`** | `long id`, `long actorUserId`, `String action`, `String targetType`, `String targetId`, `Instant createdAt` | Actor-linked governance audit entry |
| **`AccountActionRequest`** *(P1)* | `long id`, `long accountId`, `AccountActionType actionType`, `String reason`, `long requestedByUserId`, `RequestStatus status`, `Long reviewedByUserId`, `Instant createdAt`, `Instant reviewedAt` | Optional maker-checker entity |

---

## 2. Presentation-Facing DTOs (`com.tiet.banking.dto`)

Co-owned by **S** and **R**. JavaFX consumes these DTOs exclusively (zero `passwordHash`/`passwordSalt` exposure, zero JDBC types).

| DTO | Fields & Java Types | Purpose |
| :--- | :--- | :--- |
| **`UserSession`** | `long userId`, `String fullName`, `String email`, `Role role`, `Long accountId`, `String accountNumber` | Returned on login/setup/register; `accountId`/`accountNumber` are `null` for `STAFF`/`ADMIN` |
| **`AccountSummary`** | `long accountId`, `String accountNumber`, `long ownerUserId`, `String ownerName`, `String ownerEmail`, `long balancePaise`, `String formattedBalance`, `AccountStatus status`, `Instant createdAt` | Customer dashboard & Staff/Admin read-only account view |
| **`TransactionReceipt`** | `String referenceId`, `OperationType operationType`, `String accountNumber`, `String counterpartyAccountNumber`, `long amountPaise`, `String formattedAmount`, `long balanceAfterPaise`, `String formattedBalanceAfter`, `Instant timestamp` | Returned after deposit, withdrawal, or transfer (`counterpartyAccountNumber` nullable on cash ops) |
| **`TransactionView`** | `String referenceId`, `OperationType operationType`, `long deltaPaise`, `String formattedDelta`, `long balanceAfterPaise`, `String formattedBalanceAfter`, `String performedByRole`, `Instant timestamp` | Row model for JavaFX `TableView` transaction history |
| **`CustomerSearchResult`** | `long userId`, `String fullName`, `String email`, `boolean userActive`, `String accountNumber`, `AccountStatus accountStatus`, `long balancePaise`, `String formattedBalance` | Row model for Staff/Admin customer lookup table |
| **`StaffUserSummary`** | `long userId`, `String fullName`, `String email`, `boolean active`, `Instant createdAt` | Safe Staff listing DTO for Admin UI (never exposes password hash/salt) |
| **`AdministrativeSummary`** | `long totalCustomers`, `long totalActiveStaff`, `long totalAccounts`, `long frozenAccounts`, `long totalOperationsCount`, `long totalSystemBalancePaise`, `String formattedTotalBalance` | Read-only metrics for Admin dashboard |
| **`ReconciliationResult`** *(P1)* | `long accountId`, `String accountNumber`, `long storedBalancePaise`, `long expectedBalancePaise`, `long discrepancyPaise`, `boolean matched` | Non-destructive reconciliation row |
| **`AuditEventView`** *(P1)* | `long auditId`, `long actorUserId`, `String actorEmail`, `Role actorRole`, `String action`, `String targetType`, `String targetId`, `Instant timestamp` | Admin Audit Viewer row |
| **`AccountActionRequestView`** *(P1)* | `long requestId`, `String accountNumber`, `String ownerName`, `AccountActionType actionType`, `String reason`, `String requestedByEmail`, `RequestStatus status`, `String reviewedByEmail`, `Instant createdAt`, `Instant reviewedAt` | P1 freeze/unfreeze approval queue row |

---

## 3. Exception Contract (`com.tiet.banking.exception`)

Owned by **S** (runtime exceptions extending a base `BankingException` to keep service/UI signatures clean):
- `ValidationException` — malformed email/name/password/amount, zero/negative money, scale `> 2`, self-transfer
- `AuthenticationException` — invalid credentials or deactivated user (`is_active == false`)
- `AuthorizationException` — unauthenticated call, disallowed `Role`, or ownership/maker-checker violation
- `ResourceNotFoundException` — account number, user ID, or request ID not found
- `InsufficientFundsException` — debit exceeds available `balancePaise`
- `AccountFrozenException` — financial mutation attempted on `FROZEN` account
- `ConflictException` — duplicate email, repeated bootstrap admin setup, or P1 duplicate request/idempotency conflict
- `PersistenceException` — wrapped JDBC/SQLite failure (never leaks raw SQL to UI)
- `CommitOutcomeUncertainException` *(extends `PersistenceException`)* — holds nullable `String referenceId` when `commit()` fails ambiguously (`Decision E`)

---

## 4. Service Interface Contracts (`com.tiet.banking.service`)

> **Authorization Rule:** Every authenticated method reads the active `UserSession` from `SecurityContext` (`S`), verifies allowed `Role`(s), and logs rejected attempts safely via `java.util.logging` (`Decision D`). No client-supplied `idempotencyKey` appears in P0 (`Decision B`).

### 4.1 `AuthService`

| Method Signature | Caller Role | Inputs | Return | Authorization & Preconditions | Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `isInitialAdminSetupRequired()` | `UNAUTHENTICATED` | None | `boolean` | Public check; returns `true` iff `COUNT(ADMIN) == 0` | `PersistenceException` | Read-only query | Determines startup route (`Setup` vs `Login`) |
| `setupInitialAdmin(String fullName, String email, char[] password)` | `UNAUTHENTICATED` | Non-blank `fullName`, valid `email`, non-empty `password` | `UserSession` | Allowed only when `COUNT(ADMIN) == 0`; normalizes email lowercase (`Decision F`) | `ValidationException`, `ConflictException`, `PersistenceException` | **Atomic DB tx:** re-check `COUNT(ADMIN) == 0`, insert `ADMIN` user, insert `audit_events` (`INITIAL_ADMIN_CREATED`) | Initial `ADMIN` created & logged in; bootstrap permanently locked |
| `registerCustomer(String fullName, String email, char[] password)` | `UNAUTHENTICATED` | Non-blank `fullName`, valid `email`, non-empty `password` | `UserSession` | Public registration; role fixed to `CUSTOMER`; email unused | `ValidationException`, `ConflictException`, `PersistenceException` | **Atomic DB tx:** insert `CUSTOMER` user + `AccountDao.createAccount` (`0` paise, `ACTIVE`, `AC000001`) | Customer + zero-balance account created; customer logged in |
| `login(String email, char[] password)` | `UNAUTHENTICATED` | Non-blank `email`, `password` | `UserSession` | User exists, `active == true`, salted PBKDF2 hash verifies | `ValidationException`, `AuthenticationException`, `PersistenceException` | Read-only lookup | Sets active `UserSession` in `SecurityContext` |
| `logout()` | Authenticated | None | `void` | Clears current session (safe no-op if unauthenticated) | None | None | Clears `SecurityContext` |
| `getCurrentSession()` | Any | None | `Optional<UserSession>` | None | None | None | Returns current active session if present |

### 4.2 `CustomerBankingService`

| Method Signature | Caller Role | Inputs | Return | Authorization & Preconditions | Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `getOwnAccountSummary()` | `CUSTOMER` | None | `AccountSummary` | Must be `CUSTOMER`; looks up account strictly by session `userId` (`INV-06`) | `AuthorizationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only query | Returns customer's single account summary |
| `getOwnTransactionHistory(int limit)` | `CUSTOMER` | `limit > 0 && limit <= 500` | `List<TransactionView>` | Must be `CUSTOMER`; resolves own `accountId` from session | `AuthorizationException`, `ValidationException`, `PersistenceException` | Read-only query | Newest-first journal rows for customer's own account |
| `transferFunds(String destinationAccountNumber, String amountInput)` | `CUSTOMER` | Non-blank `destinationAccountNumber`, positive `Money` string `amountInput` | `TransactionReceipt` | Must be `CUSTOMER`; source resolved from session `userId`; `src != dst`; both accounts `ACTIVE`; `src.balance >= amount` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `AccountFrozenException`, `InsufficientFundsException`, `CommitOutcomeUncertainException`, `PersistenceException` | **Atomic DB tx:** read both accounts, conditional debit + credit (`rowsAffected==1`), insert `1` `TRANSFER` operation + `2` `transactions` rows summing to `0` | Source debited, destination credited atomically; receipt returned |
| `exportOwnStatementCsv(Path targetFile)` *(P1)* | `CUSTOMER` | Non-null writable `targetFile` | `Path` | Must be `CUSTOMER`; exports own account history only | `AuthorizationException`, `ValidationException`, `PersistenceException` | Read-only query + Java I/O | CSV file written safely to `targetFile` |

### 4.3 `TellerService`

| Method Signature | Caller Role | Inputs | Return | Authorization & Preconditions | Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `depositCash(String accountNumber, String amountInput)` | `STAFF` | Non-blank `accountNumber`, positive `Money` string `amountInput` | `TransactionReceipt` | Must be `STAFF` (`ADMIN` & `CUSTOMER` denied); target account exists & `ACTIVE` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `AccountFrozenException`, `CommitOutcomeUncertainException`, `PersistenceException` | **Atomic DB tx:** check overflow (`Math.addExact`), `creditIfActive`, insert `DEPOSIT` operation + `1` positive `transactions` row | Account balance incremented; receipt returned |
| `withdrawCash(String accountNumber, String amountInput)` | `STAFF` | Non-blank `accountNumber`, positive `Money` string `amountInput` | `TransactionReceipt` | Must be `STAFF`; target account exists, `ACTIVE`, and `balance >= amount` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `AccountFrozenException`, `InsufficientFundsException`, `CommitOutcomeUncertainException`, `PersistenceException` | **Atomic DB tx:** `debitIfActiveAndSufficient`, insert `WITHDRAWAL` operation + `1` negative `transactions` row | Account balance decremented; receipt returned |

### 4.4 `StaffService`

| Method Signature | Caller Role | Inputs | Return | Authorization & Preconditions | Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `searchCustomers(String query)` | `STAFF` or `ADMIN` *(Read-Only — Decision C)* | `query` (nullable/blank allowed for bounded listing, trimmed) | `List<CustomerSearchResult>` | Must be `STAFF` or `ADMIN` (`CUSTOMER` denied) | `AuthorizationException`, `PersistenceException` | Read-only parameterized `LIKE` query (`role = 'CUSTOMER'`) | Matching customer & account rows returned |
| `getCustomerAccountSummary(String accountNumber)` | `STAFF` or `ADMIN` *(Read-Only — Decision C)* | Non-blank `accountNumber` | `AccountSummary` | Must be `STAFF` or `ADMIN`; account exists | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only query | Returns customer account summary |
| `getCustomerTransactionHistory(String accountNumber, int limit)` | `STAFF` or `ADMIN` *(Read-Only — Decision C)* | Non-blank `accountNumber`, `limit > 0 && limit <= 500` | `List<TransactionView>` | Must be `STAFF` or `ADMIN`; account exists | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only query | Returns newest-first transactions for account |

### 4.5 `AdminService`

| Method Signature | Caller Role | Inputs | Return | Authorization & Preconditions | Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `createStaffUser(String fullName, String email, char[] password)` | `ADMIN` | Non-blank `fullName`, valid `email`, non-empty `password` | `StaffUserSummary` | Must be `ADMIN`; email unused | `AuthorizationException`, `ValidationException`, `ConflictException`, `PersistenceException` | **Atomic DB tx:** insert `STAFF` user + insert `audit_events` (`STAFF_CREATED`) | Staff identity provisioned & audited |
| `setStaffActiveStatus(long staffUserId, boolean active)` | `ADMIN` | `staffUserId > 0`, `active` | `void` | Must be `ADMIN`; target user exists and has `role == STAFF` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `PersistenceException` | **Atomic DB tx:** update `users.is_active` + insert `audit_events` (`STAFF_ACTIVATED`/`STAFF_DEACTIVATED`) | Staff account enabled/disabled & audited |
| `listStaffUsers()` | `ADMIN` | None | `List<StaffUserSummary>` | Must be `ADMIN` | `AuthorizationException`, `PersistenceException` | Read-only query (`role = 'STAFF'`) | Returns safe staff list |
| `getAdministrativeSummary()` | `ADMIN` | None | `AdministrativeSummary` | Must be `ADMIN` | `AuthorizationException`, `PersistenceException` | Read-only aggregate query | Returns system counts & total simulated balance |
| `getAuditEvents(int limit, String actionFilter)` *(P1 UI)* | `ADMIN` | `limit > 0 && limit <= 500`, optional `actionFilter` | `List<AuditEventView>` | Must be `ADMIN` | `AuthorizationException`, `ValidationException`, `PersistenceException` | Read-only query (`audit_events JOIN users`) | Populates Admin Audit Viewer |

### 4.6 P1 Services (`ReconciliationService`, `AccountControlService`)

| Method Signature | Caller Role | Inputs | Return | Authorization & Preconditions | Exceptions | Transaction Requirement | Observable Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `ReconciliationService.reconcileAccount(String accountNumber)` | `ADMIN` | Non-blank `accountNumber` | `ReconciliationResult` | Must be `ADMIN`; account exists | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `PersistenceException` | Read-only tx comparing `balance_paise` vs `SUM(delta_paise)` | Reports match/discrepancy without altering balances (`INV-11`) |
| `ReconciliationService.reconcileAllAccounts()` | `ADMIN` | None | `List<ReconciliationResult>` | Must be `ADMIN` | `AuthorizationException`, `PersistenceException` | Read-only tx across all accounts | Returns system-wide reconciliation list |
| `AccountControlService.requestAccountFreezeChange(String accountNumber, AccountActionType actionType, String reason)` | `STAFF` | Non-blank `accountNumber`, `actionType`, non-blank `reason` | `AccountActionRequestView` | Must be `STAFF`; no existing `PENDING` request; action changes current status | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `ConflictException`, `PersistenceException` | **Atomic DB tx:** insert `account_action_requests` (`PENDING`) + insert `audit_events` | Queues request for Admin review |
| `AccountControlService.listPendingRequests()` | `ADMIN` or `STAFF` | None | `List<AccountActionRequestView>` | Must be `ADMIN` or `STAFF` | `AuthorizationException`, `PersistenceException` | Read-only query | Lists `PENDING` requests |
| `AccountControlService.reviewAccountFreezeChange(long requestId, boolean approve)` | `ADMIN` | `requestId > 0`, `approve` | `AccountActionRequestView` | Must be `ADMIN`; request is `PENDING`; `reviewerId != requestedByUserId` | `AuthorizationException`, `ValidationException`, `ResourceNotFoundException`, `ConflictException`, `PersistenceException` | **Atomic DB tx:** update request decision; if approved update `accounts.status`; insert `audit_events` | Updates status & audit atomically |

---

## 5. Frozen `S ↔ P` Transaction & DAO Contracts (`com.tiet.banking.db`, `com.tiet.banking.dao`)

### 5.1 Transaction Coordinator (`db/`)
- `TransactionExecutor`: `<T> T execute(TransactionWork<T> work)` and `void executeVoid(Consumer<BankingUnitOfWork> work)` (optional convenience). Manages single SQLite `Connection`, `PRAGMA foreign_keys = ON`, `autoCommit = false`, `commit()`, `rollback()`, cleanup, and throws `CommitOutcomeUncertainException` on `commit()` failure.
- `TransactionWork<T>`: `@FunctionalInterface public interface TransactionWork<T> { T execute(BankingUnitOfWork uow); }`
- `BankingUnitOfWork`: exposes `users()`, `accounts()`, `operations()`, `transactions()`, `auditEvents()` (and P1 `accountActionRequests()`), all bound to the active transaction `Connection`.

### 5.2 DAO Signatures (`dao/`)
*(Read queries may execute via `TransactionExecutor` or direct read connection with `PRAGMA foreign_keys = ON`.)*

- **`UserDao`:**
  - `User insert(User user)` -> assigns generated `id`
  - `Optional<User> findById(long id)`
  - `Optional<User> findByEmail(String normalizedEmail)`
  - `long countByRole(Role role)`
  - `List<User> findByRole(Role role)`
  - `List<CustomerSearchResult> searchCustomers(String query)`
  - `boolean updateActiveStatus(long userId, boolean active)`
- **`AccountDao`:**
  - `BankAccount createAccount(long userId, Instant createdAt)` -> atomically inserts zero-balance `ACTIVE` row, reads `id`, assigns `AC%06d` (`AC000001`), returns populated `BankAccount` (`Decision A`)
  - `Optional<BankAccount> findById(long accountId)`
  - `Optional<BankAccount> findByUserId(long userId)`
  - `Optional<BankAccount> findByAccountNumber(String accountNumber)`
  - `boolean creditIfActive(long accountId, long positiveDeltaPaise)` -> SQL `WHERE id = ? AND status = 'ACTIVE'`
  - `boolean debitIfActiveAndSufficient(long accountId, long positiveDebitPaise)` -> SQL `WHERE id = ? AND status = 'ACTIVE' AND balance_paise >= ?`
  - `boolean updateStatus(long accountId, AccountStatus status)` *(P1)*
  - `AdministrativeSummary getAdministrativeSummary()`
- **`OperationDao`:** *(Append + Read only — `INV-09`)*
  - `BankOperation insert(BankOperation operation)` -> assigns generated `id`
  - `Optional<BankOperation> findByReferenceId(String referenceId)`
- **`TransactionDao`:** *(Append + Read only — `INV-09`)*
  - `BankTransaction insert(BankTransaction tx)` -> assigns generated `id`
  - `List<TransactionView> findViewsByAccountId(long accountId, int limit)`
  - `long sumDeltasByAccountId(long accountId)` *(P1 reconciliation)*
- **`AuditEventDao`:** *(Append + Read only — `INV-10`)*
  - `AuditEvent insert(AuditEvent event)` -> assigns generated `id`
  - `List<AuditEventView> findRecent(int limit, String actionFilter)` *(P1 UI)*

---

## 6. M1 Contract Readiness Summary (Decisions A–F Resolved)

All previously flagged M1 questions are now technically resolved across `S`, `P`, and `R`:
- **Decision A:** `AccountDao.createAccount` assigns `AC000001` from generated `accounts.id` inside the transaction.
- **Decision B:** P0 omits `idempotencyKey` from service signatures and schema; every operation still gets a unique server `reference_id`.
- **Decision C:** `ADMIN` receives read-only access to `StaffService` customer search/view methods while remaining barred from teller cash actions.
- **Decision D:** Sensitive mutations log `audit_events` inside the same DB transaction; rejected attempts log via `java.util.logging`.
- **Decision E:** `CommitOutcomeUncertainException` prevents automatic financial retries and directs users to inspect history / `referenceId`.
- **Decision F:** First-run `ADMIN` setup locks atomically inside `TransactionExecutor`.
