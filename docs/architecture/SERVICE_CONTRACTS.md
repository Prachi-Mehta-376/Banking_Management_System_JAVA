# Service & DAO Contracts (`SERVICE_CONTRACTS.md`)

> **Status:** `PROVISIONAL CONCEPTUAL CONTRACTS — NO JAVA SIGNATURES FINALIZED`
> **Rule:** Do not implement against these boundaries as frozen APIs until **S**, **P**, and **R** finalize exact Java interfaces during **M1**.

---

## 1. Purpose of This Document

This document defines the **conceptual responsibilities and boundaries** between:
1. **JavaFX Presentation (`R`)** <-> **Business Services (`S`)** via Service interfaces & DTOs.
2. **Business Services (`S`)** <-> **Persistence (`P`)** via DAO interfaces, domain models, and transaction coordination.

---

## 2. Presentation <-> Service Contracts (`R` <-> `S`)

All JavaFX views communicate exclusively with Business Services using DTOs/domain read views and catch structured domain/authorization exceptions.

### 2.1 Authentication & Session Service (`AuthService` concept)
- **Capabilities (P0):**
  - Authenticate username + password -> return authenticated session/user summary DTO (`userId`, `username`, `role`, optional `customerId`).
  - Logout / clear active session context.
  - Retrieve current authenticated principal details.
- **Expected Failure Semantics:** Invalid credentials error, disabled account error.

### 2.2 Customer Onboarding & Profile Service (`CustomerService` concept)
- **Capabilities (P0):**
  - Register a new synthetic customer + user login credentials (Staff-initiated).
  - Search/lookup synthetic customer profiles (Staff/Admin) or fetch own customer profile (Customer).
- **Ownership / Role Rule:** Customers may only read their own profile.

### 2.3 Account Management & Inquiry Service (`AccountService` concept)
- **Capabilities (P0):**
  - Create a new simulated account for an existing synthetic customer (Staff-initiated).
  - Retrieve accounts owned by the logged-in customer (Customer) or inspect customer accounts (Staff/Admin).
  - Retrieve current balance and account status for authorized viewers.
- **Expected Failure Semantics:** Unauthorized access / ownership mismatch, customer not found.

### 2.4 Financial Transaction Service (`TransactionService` concept)
- **Capabilities (P0):**
  - **Simulated Cash Deposit (Staff only):** Credit positive amount to an `ACTIVE` customer account, write ledger row, write audit entry atomically.
  - **Simulated Cash Withdrawal (Staff only):** Verify sufficient balance and `ACTIVE` status, debit customer account, write ledger row, write audit entry atomically.
  - **Customer Account Transfer (Customer only, for owned active source account):** Verify source ownership, verify destination exists and is `ACTIVE`, verify source != destination, verify positive amount and sufficient balance, perform atomic debit + credit + ledger + audit.
  - **Transaction History Query (P0):** Return chronological list of transactions for an authorized account.
- **Capabilities (P1 / P2):**
  - **Statement Export (P1):** Export authorized account statement using Java I/O to user-chosen file path.
  - **Balance Reconciliation (P1, Admin):** Verify account balance equals net ledger activity.
  - **Duplicate Request / Risk Check (P2):** Pending Gemini Deep Research review.

### 2.5 Governance, Freeze Requests & Audit Service (`AdminGovernanceService` / `AuditService` concepts — P1)
- **Capabilities (P1):**
  - Submit freeze or unfreeze request with reason (Bank Staff).
  - List pending freeze/unfreeze requests (Admin, Staff read-only).
  - Approve or reject pending request (Admin only, preventing self-approval).
  - Create and query Staff user accounts (Admin only).
  - Query system-wide audit logs with role/action/date filtering (Admin only).

---

## 3. Service <-> Persistence Contracts (`S` <-> `P`)

Business services rely on **DAO interfaces** (`com.tiet.banking.dao`) and a **Transaction Coordinator** (`com.tiet.banking.db`), never raw SQL strings.

### 3.1 Transaction Coordinator Contract (Conceptual — `RISK-01`)
- Provides a mechanism for **S** to execute a multi-DAO unit of work inside a single SQLite `Connection` with `setAutoCommit(false)`, automatic `commit()` on success, and guaranteed `rollback()` on any exception.

### 3.2 Proposed DAO Interfaces (Conceptual)
- `UserDao`: Find by username/id, insert user, update enabled status, list staff users.
- `CustomerDao`: Insert synthetic customer, find by ID / userId / synthetic code, search by name/code.
- `AccountDao`: Insert account, find by ID / account number / customer ID, update balance conditionally/atomically within active transaction, update account status.
- `TransactionDao`: Append immutable transaction record, query by account ID (ordered newest-first), aggregate net credits/debits for reconciliation.
- `FreezeRequestDao` (P1): Insert request, find pending requests, update decision/reviewer/timestamp.
- `AuditLogDao` (P1): Append immutable audit entry, query filtered audit records.

---

## 4. DTO & Mock Strategy for Parallel Development

- **Shared DTOs (`com.tiet.banking.dto`):** Immutable request and summary objects (e.g., LoginRequest, TransferRequest, AccountSummary, TransactionView, AuditLogView) agreed jointly by **S** and **R** in **M1**.
- **UI Decoupling:** Because `R` depends strictly on service interfaces and DTOs, `R` can verify JavaFX views using temporary mock/in-memory implementations of those interfaces without waiting for SQLite integration (`M3`).
