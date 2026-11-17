# Planned Academic Demo Scenarios (`DEMO_SCENARIOS.md`)

> **Version:** M1 v2.0 Specification (`PLANNED — APPLICATION NOT YET IMPLEMENTED`)
> **Note:** Use strictly fictitious customer names and simulated currency values during live evaluation. Do not document fake screenshots or credentials before implementation and verification.

---

## 1. Purpose

These walkthrough scripts outline how the team plans to demonstrate the **Banking Management System** during the **UCS553** project evaluation once **M2–M4** implementation and verification are completed.

---

## 2. Planned Evaluation Walkthroughs

### Scenario 1 (`DEMO-01`): First-Run Admin Setup & Staff Provisioning (`P0`)
- **Actors:** Initial Administrator (`ADMIN`)
- **Planned Steps:**
  1. Launch application against a clean SQLite database -> observe first-run Administrator Setup screen.
  2. Enter Admin details and user-supplied password -> verify routing to Admin Dashboard and confirmation that first-run setup is permanently locked.
  3. Create a synthetic Bank Staff account (`STAFF`) and inspect `AdministrativeSummary`.
  4. Log out cleanly.
- **Status:** `PLANNED — NOT EXECUTED`

### Scenario 2 (`DEMO-02`): Public Customer Registration & Staff Cash Operations (`P0`)
- **Actors:** Customer A (fictitious), Bank Staff (`STAFF`)
- **Planned Steps:**
  1. From Login screen, use Public Customer Registration to create Customer A -> verify automatic creation of 1 `ACTIVE` account at `₹0.00` balance, then log out.
  2. Log in as Bank Staff -> search Customer A by email/account number.
  3. Deposit `₹1,000.00` simulated cash into Customer A's account -> withdraw `₹200.00` -> verify updated balance `₹800.00` and `TableView` transaction history.
- **Status:** `PLANNED — NOT EXECUTED`

### Scenario 3 (`DEMO-03`): Customer Transfer, Authorization Guard & Persistence Restart (`P0`)
- **Actors:** Customer A, Customer B (fictitious)
- **Planned Steps:**
  1. Log in as Customer A -> view own `₹800.00` balance and history.
  2. Attempt invalid transfer (`₹1,500.00` or `-50.00`) -> observe clean validation/insufficient-funds feedback and unchanged balance.
  3. Transfer `₹300.00` to Customer B -> confirm Customer A now shows `₹500.00` and journal row `-₹300.00`.
  4. Close and restart the JavaFX application -> log in as Customer B and confirm `₹300.00` balance and `+₹300.00` history persisted on disk.
- **Status:** `PLANNED — NOT EXECUTED`

### Scenario 4 (`DEMO-04`): P1 Differentiating Controls (`P1`)
- **Actors:** Customer A, Bank Staff, Administrator
- **Planned Steps:**
  1. Customer A exports statement CSV via Java I/O (`F-P1-04`).
  2. Staff submits a freeze request for Customer A (`F-P1-03`); Admin approves it -> verify Customer A can still view balance/history while deposits/withdrawals/transfers are rejected.
  3. Admin inspects Audit Events viewer (`F-P1-02`) and executes Balance Reconciliation (`F-P1-01`).
- **Status:** `PLANNED — NOT EXECUTED`

### Scenario 5 (`DEMO-05`): Automated Test & Atomic Rollback Viva Walkthrough (`P0`)
- **Actors:** Team (`S`, `P`, `R`)
- **Planned Walkthrough:**
  1. Execute JUnit test suite (`TC-01` through `TC-14`) demonstrating `TransactionExecutor` rollback on injected mid-transfer failure (`TC-10`, `TC-11`) and concurrent withdrawal safety (`TC-12`).
  2. Show `JavaFX -> Service -> DAO -> SQLite` layering and explain integer-paise storage + `BigDecimal` parsing (`INV-01`–`INV-12`).
- **Status:** `PLANNED — NOT EXECUTED`
