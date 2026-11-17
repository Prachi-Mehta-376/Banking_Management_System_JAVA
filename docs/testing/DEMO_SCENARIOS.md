# Planned Academic Demo Scenarios (`DEMO_SCENARIOS.md`)

> **Status:** `PLANNED — APPLICATION NOT YET IMPLEMENTED`
> **Note:** Use strictly synthetic customer names and simulated currency values during live evaluation. Do not document fake screenshots or credentials here until implemented and verified in M5.

---

## 1. Purpose

These walkthrough scenarios describe how the team plans to demonstrate the **Banking Management System** for the **UCS553** project evaluation once **M3/M4** implementation and testing are complete.

---

## 2. Planned Evaluation Walkthroughs

### Scenario 1 (`DEMO-01`): Bank Staff Onboarding & Simulated Cash Deposit (`P0`)
- **Actors:** Bank Staff
- **Planned Steps:**
  1. Log in as a synthetic Bank Staff user -> verify routing to Staff Dashboard.
  2. Register a new synthetic Customer profile and create a simulated Savings Account.
  3. Perform a simulated cash deposit into the new account.
  4. Verify updated balance and immutable `CASH_DEPOSIT` entry in transaction history.
- **Status:** `PLANNED — NOT EXECUTED`

### Scenario 2 (`DEMO-02`): Customer Balance Inquiry, Transfer & History (`P0`)
- **Actors:** Customer A, Customer B (synthetic)
- **Planned Steps:**
  1. Log in as Customer A -> confirm Customer A sees **only** Customer A's owned accounts.
  2. Attempt an invalid transfer (e.g., amount exceeding balance) -> verify clear JavaFX error message and unchanged balance.
  3. Execute a valid transfer to Customer B's account -> verify atomic debit on A and credit on B.
  4. Inspect `TableView` transaction history (and P1 statement export if implemented).
- **Status:** `PLANNED — NOT EXECUTED`

### Scenario 3 (`DEMO-03`): Maker-Checker Freeze/Unfreeze & Audit Oversight (`P1`)
- **Actors:** Bank Staff, Administrator
- **Planned Steps:**
  1. Bank Staff submits a freeze request on a target account with justification.
  2. Log in as Administrator -> inspect pending freeze request queue and approve the freeze.
  3. Verify transfers/withdrawals from that account are now blocked.
  4. Administrator inspects the Audit Log viewer and executes the Balance Reconciliation check.
- **Status:** `PLANNED — NOT EXECUTED`

### Scenario 4 (`DEMO-04`): Transactional Rollback & Layering Explanation (Technical Viva)
- **Actors:** Team (`S`, `P`, `R`)
- **Planned Walkthrough:**
  1. Show automated integration test proving rollback when a failure occurs mid-transfer (`TC-DB-04`).
  2. Walk evaluators through `ui/` -> `service/` -> `dao/` -> SQLite boundaries and explain why `BigDecimal` + integer `paise` prevent floating-point drift.
- **Status:** `PLANNED — NOT EXECUTED`
