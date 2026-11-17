# Role Permissions, Bootstrap & Account Lifecycle (`ROLE_PERMISSIONS.md`)

> **Version:** M1 v2.0 Specification (`PROPOSED — AWAITING TEAM REVIEW`)
> **Related Decisions:** `ADR-005` (Three-Role RBAC & Non-Teller Admin), `ADR-008` (Bootstrap, Registration & v1 Account States).

---

## 1. Access Control Philosophy

Authorization is enforced inside the **Business Service & Security Layer (`S`)** using the active `UserSession` (`INV-05`, `INV-06`). JavaFX (`R`) routes users and hides irrelevant navigation items for usability, **never** as a security substitute.

Key M1 principles:
- **Public Customer Registration:** Public registration creates **only** `CUSTOMER` identities paired atomically with one zero-balance `ACTIVE` bank account. Callers cannot supply a role.
- **No Privilege Escalation:** Customers cannot grant themselves `STAFF` or `ADMIN` privileges.
- **Non-Teller Administrator Rule:** `ADMIN` governs staff accounts, statistics, audits, and P1 approvals, and does **not** automatically receive permission to deposit, withdraw, or transfer customer funds.
- **Single-Account Ownership in v1:** A `CUSTOMER` owns at most one account (`UNIQUE(user_id)`) and can view or debit **only** that account.

---

## 2. First-Run Administrator Bootstrap Proposal (`ADR-008`)

Because the repository must **never** ship reusable hardcoded admin passwords (`admin123`, etc.), initial administrator setup works as follows:

1. **Startup Check (`AuthService.isInitialAdminSetupRequired()`):** Queries `SELECT COUNT(*) FROM users WHERE role = 'ADMIN'`.
2. **Conditional Setup Mode:**
   - If `count == 0`, JavaFX opens a one-time **Initial Administrator Setup** screen before normal login.
   - The operator enters `fullName`, `email`, and a **user-supplied strong password**.
3. **Atomic Persistence & Lockout:**
   - `AuthService.setupInitialAdmin(...)` re-checks `count == 0` inside a database transaction (`TransactionExecutor`), hashes the password with salted `PBKDF2`, inserts the `ADMIN` row, and records `INITIAL_ADMIN_CREATED` in `audit_events` (`INV-10`).
   - Once at least one `ADMIN` exists, `setupInitialAdmin` **permanently rejects** subsequent calls with `ConflictException` (`409` semantics).
4. **Failure Handling:** Invalid password/email input fails validation before persistence without leaving half-created admin rows; if DB initialization fails, startup logs cleanly and prompts recovery without opening an unauthenticated admin dashboard.

---

## 3. Complete Role-Permission Matrix (M1 v2.0)

| Capability | Priority | Unauthenticated | Customer | Bank Staff | Administrator | Ownership / Precondition Rules |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| Check Initial Admin Setup Needed | `P0` | Allowed | Allowed | Allowed | Allowed | Read-only check |
| Setup Initial Administrator | `P0` | Allowed *(1st run only)* | **DENIED** | **DENIED** | **DENIED** | Permanently disabled once `COUNT(ADMIN) > 0` |
| Public Customer Registration | `P0` | Allowed | **DENIED** | **DENIED** | **DENIED** | Hardcodes `role = CUSTOMER`, creates 1 `ACTIVE` account at `₹0.00` |
| Login / Logout | `P0` | Login Allowed | Logout Allowed | Logout Allowed | Logout Allowed | Requires `users.is_active == 1` |
| View Own Account Summary & Balance | `P0` | **DENIED** | Allowed (Own) | N/A *(No account)* | N/A *(No account)* | Enforced via session `userId` (`INV-06`) |
| View Own Transaction History | `P0` | **DENIED** | Allowed (Own) | N/A | N/A | Enforced via session `userId` |
| Internal Account Transfer | `P0` | **DENIED** | Allowed (Own Src) | **DENIED** | **DENIED** | Source from session; both accounts `ACTIVE` |
| Search Customers / Accounts | `P0` | **DENIED** | **DENIED** | Allowed | **DENIED** *(or Read-Only; confirm M1)* | Searches `role = 'CUSTOMER'` accounts |
| View Specific Customer Account & History | `P0` | **DENIED** | **DENIED** | Allowed | **DENIED** *(Uses stats/audit)* | Authorized staff only |
| Simulated Cash Deposit | `P0` | **DENIED** | **DENIED** | Allowed | **DENIED** | Target account must be `ACTIVE` |
| Simulated Cash Withdrawal | `P0` | **DENIED** | **DENIED** | Allowed | **DENIED** | Target account `ACTIVE` & `balance >= amount` |
| Create / Deactivate Staff Accounts | `P0` | **DENIED** | **DENIED** | **DENIED** | Allowed | Recorded in `audit_events` (`INV-10`) |
| View Administrative Statistics | `P0` | **DENIED** | **DENIED** | **DENIED** | Allowed | Aggregate totals only |
| Export Own Statement CSV | `P1` | **DENIED** | Allowed (Own) | **DENIED** | **DENIED** | Uses Java I/O |
| View System Audit Events | `P1` | **DENIED** | **DENIED** | **DENIED** | Allowed | Read-only audit viewer |
| Run Balance Reconciliation | `P1` | **DENIED** | **DENIED** | **DENIED** | Allowed | Non-destructive comparison (`INV-11`) |
| Request Account Freeze / Unfreeze | `P1` | **DENIED** | **DENIED** | Allowed | **DENIED** | Staff acts as Maker |
| Approve / Reject Freeze / Unfreeze | `P1` | **DENIED** | **DENIED** | **DENIED** | Allowed | Admin acts as Checker; `reviewer != requester` |

---

## 4. Account States & Frozen-Account Policy (v1)

For **v1**, accounts support exactly two states (`AccountStatus`):
- **`ACTIVE`** — Eligible for balance enquiry, transaction history, deposits, withdrawals, and incoming/outgoing transfers.
- **`FROZEN`** — Account restricted pending administrative unfreeze. **Account closure is explicitly out of P0 scope.**

### Explicit `FROZEN` Account Behavior (`INV-07`)
When an account has `status = FROZEN`:
- **Customer View Own Account & History:** **ALLOWED** (read-only transparency).
- **Simulated Cash Deposit:** **REJECTED** (`AccountFrozenException`).
- **Simulated Cash Withdrawal:** **REJECTED** (`AccountFrozenException`).
- **Outgoing Transfer (from frozen account):** **REJECTED** (`AccountFrozenException`).
- **Incoming Transfer (into frozen account):** **REJECTED** (`AccountFrozenException`).

### P1 Maker-Checker Account-Control Workflow
1. **Bank Staff** submits a `FREEZE` or `UNFREEZE` request with a written reason (`AccountControlService.requestAccountFreezeChange`).
2. **Different Authorized Reviewer (`ADMIN`)** reviews the `PENDING` request (`reviewAccountFreezeChange`), blocked from self-approval (`reviewerUserId != requestedByUserId`).
3. On approval, `accounts.status`, `account_action_requests.status`, and `audit_events` update atomically inside a single `TransactionExecutor` transaction.
