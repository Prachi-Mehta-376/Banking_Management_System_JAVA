# Current Status (`CURRENT_STATUS.md`)

> **Last Updated:** 2026-11-25 (Final Release Preparation — Maven Wrapper, Portable Onboarding & Clean-Checkout Verification)
> **Rule:** Never report code, builds, or tests as passing unless observed in the current repository.

---

## 1. High-Level Status Summary

| Workstream | Status | Notes |
| :--- | :--- | :--- |
| **M0 Repository & Context Foundation** | `COMPLETE` | Committed on `main` (`3929940`) |
| **M1 Architecture v2.0 & Contract Spec** | `COMPLETE` | Merged into `main` via PR #1 (`5b9fad2`) |
| **Maven Configuration & Foundation (`pom.xml`)** | `IMPLEMENTED & VERIFIED` | Java 17, JavaFX 17.0.12, `sqlite-jdbc` 3.46.1.0, JUnit 5.10.3 |
| **Database & JDBC Persistence (`db/`, `dao/`, `schema.sql`)** | `IMPLEMENTED & VERIFIED` | 7 tables, `TransactionExecutor`, `AC000001` atomic assignment, `PRAGMA foreign_keys = ON` |
| **Auth, Security & Core Banking Engine (`security/`, `service/`)** | `IMPLEMENTED & VERIFIED` | Salted `PBKDF2`, RBAC, `Money` paise arithmetic, all 7 services wired in `ServiceRegistry` |
| **JavaFX Desktop Application (`ui/*`, `banking.css`)** | `IMPLEMENTED & VERIFIED` | Auth views + Customer, Staff & Admin dashboards + Integrity Control Center |
| **Automated & Live GUI Tests (`src/test/`)** | `PASSED (12/12)` | `mvn clean test`: 12 tests run, 0 failures, 0 errors, 0 skipped; 12 genuine PNG screenshots captured |
| **Optional P2 Extensions** | `DEFERRED` | Fraud alerts, disputes, and multi-account types deferred per priority policy |
| **Gemini Deep Research** | `PENDING REVIEW` | External research remains unpopulated; zero fabricated regulatory claims |

---

## 2. Current Verification Snapshot

- **Build & Test:** `./mvnw clean test` passes (`12` tests across `5` test classes, `0` failures/errors/skipped); non-GUI suite `./mvnw test -DexcludedGroups=gui` passes (`11` tests).
- **Portability & GUI Smoke Launch:** Official Apache Maven Wrapper (`mvnw` / `mvnw.cmd`) added (`only-script`, Maven 3.9.9); `LiveGuiVerificationAndScreenshotTest` isolates test screenshots to `@TempDir` (`@Tag("gui")`); JavaFX launches cleanly against an isolated DB and auto-initializes all 7 SQLite tables.
- **Report Evidence:** Prepared [../docs/report/TECHNICAL_REPORT_EVIDENCE.md](../docs/report/TECHNICAL_REPORT_EVIDENCE.md) and 12 live PNG screenshots under `docs/report/screenshots/`.
