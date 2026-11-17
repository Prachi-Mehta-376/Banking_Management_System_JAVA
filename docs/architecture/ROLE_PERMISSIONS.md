# Role Permissions & Access Control Model (`ROLE_PERMISSIONS.md`)

> **Status:** `PROPOSED SPECIFICATION — PENDING M1 APPROVAL`
> **Related Decision:** `ADR-005` in [../../context/DECISIONS.md](../../context/DECISIONS.md)

---

## 1. Access Control Philosophy

Authorization follows two mandatory rules enforced in the **Business Service & Security Layer (`S`)**:
1. **Role-Based Access Control (RBAC):** Every operation specifies which of the three roles (`CUSTOMER`, `STAFF`, `ADMIN`) is permitted to invoke it.
2. **Resource Ownership & Separation of Duties:**
   - **Customer Ownership:** A `CUSTOMER` may read or debit **only** accounts linked to their own `customerId`.
   - **Maker-Checker Governance (P1):** `STAFF` can propose account freeze/unfreeze requests, while only `ADMIN` can approve or reject them. An administrator cannot approve a request they submitted themselves.

UI visibility in JavaFX (`R`) reflects these permissions for usability, **never** as a replacement for service-layer authorization checks (`RISK-02`).

---

## 2. Proposed Permission Matrix

| Capability | Priority | Customer | Bank Staff | Administrator | Notes / Ownership Constraint |
| :--- | :---: | :---: | :---: | :---: | :--- |
| Login / Logout | `P0` | Allowed | Allowed | Allowed | Enabled users only |
| View Own Profile & Accounts | `P0` | Allowed (Own) | View Customer | Read-Only View | Customer strictly restricted to owned `customerId` |
| View Other Customers' Balances / History | `P0` | **DENIED** | Allowed | Allowed (Oversight) | Audited oversight access (P1) |
| Register Synthetic Customer | `P0` | **DENIED** | Allowed | Allowed (Optional) | Confirm in M1 whether Admin also registers or Staff-only |
| Create Simulated Bank Account | `P0` | **DENIED** | Allowed | **DENIED** (Proposed) | Keeps operational account opening with Staff |
| Simulated Cash Deposit | `P0` | **DENIED** | Allowed | **DENIED** | Simulated branch counter inflow |
| Simulated Cash Withdrawal | `P0` | **DENIED** | Allowed | **DENIED** | Simulated branch counter outflow; checks balance & status |
| Internal Account Transfer | `P0` | Allowed (Own Src) | **DENIED** | **DENIED** | Source account must belong to logged-in Customer |
| View Transaction History | `P0` | Allowed (Own) | Allowed | Allowed | Customer sees owned accounts only |
| Export Account Statement (Java I/O) | `P1` | Allowed (Own) | Allowed | Allowed | Customer restricted to owned accounts |
| Request Account Freeze / Unfreeze | `P1` | **DENIED** | Allowed | **DENIED** (Propose) | Staff acts as Maker; requires justification |
| Approve / Reject Freeze / Unfreeze | `P1` | **DENIED** | **DENIED** | Allowed | Admin acts as Checker; cannot self-approve |
| Manage Staff User Accounts | `P1` | **DENIED** | **DENIED** | Allowed | Create/disable Staff accounts |
| View System Audit Logs | `P1` | **DENIED** | **DENIED** | Allowed | Oversight & forensic review |
| Run Balance Reconciliation Check | `P1` | **DENIED** | **DENIED** | Allowed | Compares account balances vs. ledger sums |
| Transaction Dispute / Fraud Alert Review | `P2` | Submit (Own) | Review | Adjudicate | Subject to pending research review |

---

## 3. Unauthorized Access Behavior

When a service method detects an unauthenticated session, disallowed role, or customer ownership mismatch:
1. Abort the operation **before** invoking any balance-modifying DAO call.
2. Throw a dedicated authorization exception (`com.tiet.banking.exception`).
3. Log the denied attempt via `java.util.logging` (and audit log if independent audit persistence is chosen in `RISK-06`) without disclosing sensitive account metadata in the UI error banner.
