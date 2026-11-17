# Role Permissions, Bootstrap & Account Lifecycle (`ROLE_PERMISSIONS.md`)

> **Version:** M1 v2.0 Finalized Contract (`RECOMMENDED — AWAITING PR #1 APPROVAL`)
> **Related Decisions:** `ADR-005` (Three-Role RBAC & Non-Teller Admin), `ADR-008` (Bootstrap, Registration & v1 Account States).

---

## 1. Access Control Philosophy

Authorization is enforced inside the **Business Service & Security Layer (`S`)** using the active `UserSession` (`INV-05`, `INV-06`). JavaFX (`R`) routes users and hides irrelevant navigation items for usability, **never** as a security substitute.

Key M1 policies:
- **Public Customer Registration:** Public registration creates **only** `CUSTOMER` identities paired atomically with one zero-balance `ACTIVE` bank account (`AC000001`). Callers cannot specify `STAFF` or `ADMIN` roles.
- **Account Numbers Are Public Identifiers (`Decision A`):** `accountNumber` (`AC000001`) is a public routing identifier used for transfers and teller lookup, never an authentication secret.
- **Non-Teller Administrator Rule (`Decision C`):** `ADMIN` provisions/deactivates `STAFF` accounts, inspects administrative statistics and audits, and has **read-only** customer/account search & history lookup where needed for oversight—but **cannot** execute cash deposits, cash withdrawals, or customer transfers.
- **Single-Account Ownership in v1:** A `CUSTOMER` owns at most one account (`UNIQUE(user_id)`) and can view or debit **only** that account (`INV-06`).

---

## 2. First-Run Administrator Bootstrap (`Decision F`, `ADR-008`)

Because the repository must **never** ship reusable hardcoded admin credentials:

1. **Startup Check (`AuthService.isInitialAdminSetupRequired()`):** Checks `UserDao.countByRole(Role.ADMIN) == 0`.
2. **Conditional Setup Mode:** When `true`, JavaFX opens a one-time **Initial Administrator Setup** screen accepting `fullName`, `email`, and a **user-supplied strong password**.
3. **Concurrency-Safe Atomic Lockout:**
   - `AuthService.setupInitialAdmin(...)` opens an atomic `TransactionExecutor` transaction, re-checks `countByRole(Role.ADMIN) == 0` under the SQLite write lock, hashes the password with salted `PBKDF2WithHmacSHA256`, inserts the `ADMIN` row, and logs `INITIAL_ADMIN_CREATED` in `audit_events` (`INV-10`).
   - Once an `ADMIN` exists, any concurrent or subsequent call to `setupInitialAdmin` fails with `ConflictException`.
4. **Failure & Security Boundary:** Validation errors fail before database mutation; local SQLite remains an academic desktop simulation rather than a hardened enterprise multi-user server.

---

## 3. Complete Role-Permission Matrix (M1 v2.0)

| Capability | Priority | Unauthenticated | Customer | Bank Staff | Administrator | Ownership / Precondition Rules |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| Check Initial Admin Setup Needed | `P0` | Allowed | Allowed | Allowed | Allowed | Read-only startup check |
| Setup Initial Administrator | `P0` | Allowed *(1st run only)* | **DENIED** | **DENIED** | **DENIED** | Atomic check `COUNT(ADMIN) == 0`; disabled permanently afterward |
| Public Customer Registration | `P0` | Allowed | **DENIED** | **DENIED** | **DENIED** | Hardcodes `role = CUSTOMER`; creates 1 `ACTIVE` account at `₹0.00` |
| Login / Logout | `P0` | Login Allowed | Logout Allowed | Logout Allowed | Logout Allowed | Requires `users.is_active == 1` |
| View Own Account Summary & Balance | `P0` | **DENIED** | Allowed (Own) | N/A *(No account)* | N/A *(No account)* | Resolved strictly from session `userId` (`INV-06`) |
| View Own Transaction History | `P0` | **DENIED** | Allowed (Own) | N/A | N/A | Resolved strictly from session `userId` |
| Internal Account Transfer | `P0` | **DENIED** | Allowed (Own Src) | **DENIED** | **DENIED** | Source from session; both accounts `ACTIVE` |
| Search Customers / View Account & History | `P0` | **DENIED** | **DENIED** | Allowed | Allowed *(Read-Only — Decision C)* | Read-only inspection; cannot mutate balances |
| Simulated Cash Deposit | `P0` | **DENIED** | **DENIED** | Allowed | **DENIED** | Target account must be `ACTIVE` |
| Simulated Cash Withdrawal | `P0` | **DENIED** | **DENIED** | Allowed | **DENIED** | Target account `ACTIVE` & `balance >= amount` |
| Create / Deactivate Staff Accounts | `P0` | **DENIED** | **DENIED** | **DENIED** | Allowed | Recorded atomically in `audit_events` (`INV-10`) |
| View Administrative Statistics | `P0` | **DENIED** | **DENIED** | **DENIED** | Allowed | Aggregate totals only |
| Export Own Statement CSV | `P1` | **DENIED** | Allowed (Own) | **DENIED** | **DENIED** | Uses Java I/O |
| View System Audit Events | `P1` | **DENIED** | **DENIED** | **DENIED** | Allowed | Read-only audit viewer |
| Run Balance Reconciliation | `P1` | **DENIED** | **DENIED** | **DENIED** | Allowed | Non-destructive comparison (`INV-11`) |
| Request Account Freeze / Unfreeze | `P1` | **DENIED** | **DENIED** | Allowed | **DENIED** | Staff acts as Maker |
| Approve / Reject Freeze / Unfreeze | `P1` | **DENIED** | **DENIED** | **DENIED** | Allowed | Admin acts as Checker; `reviewer != requester` |

---

## 4. Account States & Frozen-Account Policy (v1)

For **v1**, accounts support exactly two states (`AccountStatus`): **`ACTIVE`** and **`FROZEN`**. Account closure is explicitly out of P0 scope.

### Explicit `FROZEN` Account Behavior (`INV-07`)
When an account has `status = FROZEN`:
- **Customer View Own Account & History:** **ALLOWED** (read-only transparency).
- **Simulated Cash Deposit:** **REJECTED** (`AccountFrozenException`).
- **Simulated Cash Withdrawal:** **REJECTED** (`AccountFrozenException`).
- **Outgoing Transfer (from frozen account):** **REJECTED** (`AccountFrozenException`).
- **Incoming Transfer (into frozen account):** **REJECTED** (`AccountFrozenException`).

### P1 Maker-Checker Account-Control Workflow
1. **Bank Staff** submits a `FREEZE` or `UNFREEZE` request with a written reason (`AccountControlService.requestAccountFreezeChange`).
2. **Different Authorized Reviewer (`ADMIN`)** approves or rejects the `PENDING` request (`reviewAccountFreezeChange`), blocked from self-approval (`reviewerUserId != requestedByUserId`).
3. On approval, `accounts.status`, `account_action_requests.status`, and `audit_events` update atomically inside `TransactionExecutor`.
