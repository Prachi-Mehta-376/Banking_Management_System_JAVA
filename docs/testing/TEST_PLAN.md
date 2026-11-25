# Verification & Test Plan (`TEST_PLAN.md`)

> **Last Executed:** 2026-11-25 via `mvn clean test` (`12` tests run, `0` failures, `0` errors, `0` skipped)

---

## 1. Implemented Test Suites (`src/test/java/com/tiet/banking/`)

| Suite | Scope Covered | Tests Run | Result |
| :--- | :--- | :---: | :---: |
| `service/MoneyValidationTest.java` | Valid parsing, zero/negative/`>2` decimals rejection, `long` overflow/underflow | 3 | `PASSED` |
| `dao/DatabasePersistenceTest.java` | FK/CHECK/`ON DELETE RESTRICT` constraints, `AC000001` & restart persistence, 2-thread concurrent withdrawals | 3 | `PASSED` |
| `service/AuthAndRbacTest.java` | One-time admin bootstrap (even after deactivation), salted PBKDF2, duplicate email rejection, RBAC & non-teller admin | 2 | `PASSED` |
| `integration/BankingEngineIntegrationTest.java` | Deposit/withdrawal/transfer, zero-sum journal, injected post-debit failure rollback, `CommitOutcomeUncertainException`, freeze/unfreeze maker-checker, CSV formula neutralization, reconciliation tampering detection, idempotent replay & conflict | 3 | `PASSED` |
| `integration/LiveGuiVerificationAndScreenshotTest.java` | Live JavaFX E2E workflow across all 3 roles + restart check + 12 PNG screenshots | 1 | `PASSED` |

---

## 2. Scenario Traceability (`TC-01` through `TC-17`)

| ID | Scenario | Verifying Test Suite | Status |
| :---: | :--- | :--- | :---: |
| `TC-01` | Customer registration (`AC000001`, `0` paise, duplicate email rejection) | `AuthAndRbacTest`, `DatabasePersistenceTest` | `PASSED` |
| `TC-02` | First-run administrator setup & permanent lock | `AuthAndRbacTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-03` | Staff account provisioning & deactivation | `AuthAndRbacTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-04` | Unauthorized access attempts (`CUSTOMER` & non-teller `ADMIN`) | `AuthAndRbacTest` | `PASSED` |
| `TC-05` | Valid simulated cash deposit | `BankingEngineIntegrationTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-06` | Valid simulated cash withdrawal | `BankingEngineIntegrationTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-07` | Insufficient balance rejection | `BankingEngineIntegrationTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-08` | Invalid monetary values & `long` overflow | `MoneyValidationTest` | `PASSED` |
| `TC-09` | Successful internal transfer & zero-sum deltas | `BankingEngineIntegrationTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-10` | Failure during transfer + `CommitOutcomeUncertainException` | `BankingEngineIntegrationTest` | `PASSED` |
| `TC-11` | Atomic rollback verified on fresh connection | `BankingEngineIntegrationTest` | `PASSED` |
| `TC-12` | Concurrent withdrawal safety (two threads) | `DatabasePersistenceTest` | `PASSED` |
| `TC-13` | Transaction history ordering & immutability | `DatabasePersistenceTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-14` | Persistence after application restart | `DatabasePersistenceTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-15` | Frozen-account restrictions & maker-checker approval (P1) | `BankingEngineIntegrationTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-16` | Non-destructive balance reconciliation & mismatch detection (P1) | `BankingEngineIntegrationTest`, `LiveGuiVerificationAndScreenshotTest` | `PASSED` |
| `TC-17` | Persistent idempotency replay & parameter conflict rejection (P1) | `BankingEngineIntegrationTest` | `PASSED` |
