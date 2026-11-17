# Session Handoff (`SESSION_HANDOFF.md`)

> **Use This File To:** Resume work cleanly in any new AI chat or IDE session.
> **Important:** Always inspect actual repository contents, `git status`, and PR state before relying solely on this handoff ([../AGENTS.md](../AGENTS.md)).

---

## 1. Current Project State

- **Milestone:** **M1 — Finalized Contracts on PR #1 (`docs/m1-architecture-contracts`)**, awaiting team approval and merge into `main`.
- **Stable Integration Branch (`main`):** At M0 baseline (`392994037b4f71201e445c65e7062076844196f7`).
- **Application Code:** **Not Started** (`src/` and `data/` contain `.gitkeep` placeholders only).
- **Build Configuration:** **Not Created** (`pom.xml` deferred until M2 start).
- **Tests:** **Not Implemented / Not Executed** (`17` planned scenarios in [../docs/testing/TEST_PLAN.md](../docs/testing/TEST_PLAN.md); `0` executed).

---

## 2. Most Recent Verified Work

- **M1 Finalization (On `docs/m1-architecture-contracts`):**
  - Resolved M1 contract Decisions A–F across [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md), and [ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md):
    - `AC000001` atomic account-number assignment from `accounts.id` (`AccountDao.createAccount`)
    - P0 APIs without client `idempotencyKey` (unique server `reference_id` retained; P1 idempotency deferred cleanly)
    - `ADMIN` read-only `StaffService` lookup access while remaining blocked from teller cash mutations
    - Same-transaction `audit_events` for sensitive changes + `java.util.logging` for rejections/failures
    - `CommitOutcomeUncertainException` policy with zero automatic financial retries
    - Atomic `COUNT(ADMIN) == 0` first-run setup lock
  - Froze explicit `S ↔ P` (`TransactionExecutor`, `BankingUnitOfWork`, 5 DAOs) and `S ↔ R` (Services & DTOs) method contracts.
  - Updated `ADR-001`–`ADR-008` in [DECISIONS.md](DECISIONS.md) to `RECOMMENDED — AWAITING APPROVAL`.

---

## 3. Essential Documents for the Next Session

1. [../AGENTS.md](../AGENTS.md) — Rules, student simplicity/comment discipline, Git workflow.
2. [CURRENT_STATUS.md](CURRENT_STATUS.md) & [DECISIONS.md](DECISIONS.md) — Approval gate status for `ADR-001`–`ADR-008`.
3. Authoritative Frozen Contracts: [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md), [TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md), [SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md), [ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md).
4. Teammate Handoffs: [01_S_BANKING_ENGINE.md](../docs/team_handoffs/01_S_BANKING_ENGINE.md), [02_P_DATABASE_AND_JDBC.md](../docs/team_handoffs/02_P_DATABASE_AND_JDBC.md), [03_R_JAVAFX_AND_UX.md](../docs/team_handoffs/03_R_JAVAFX_AND_UX.md).

---

## 4. Exact Resume Instructions for Future AI Sessions

```text
1. Run `git status -sb`, `git branch -a`, `git log -2 --oneline`, and `gh pr view 1` (if open).
2. Confirm whether PR #1 (`docs/m1-architecture-contracts` -> `main`) has been approved and merged by teammates S, P, and R.
3. Once PR #1 is merged (and approved ADRs transitioned to ACCEPTED), start M2 from updated `main`:
   - Step M2.0: `feature/project-foundation` (minimal Maven `pom.xml` + shared `model/`, `dto/`, `exception/`).
   - Step M2.1: Parallel branches `feature/database-jdbc` (P), `feature/banking-engine` (S), `feature/javafx-ui` (R against agreed DTOs/mocks).
4. Follow `AGENTS.md` Section 5 (simplest correct Core Java, strict comment discipline) and verify every test genuinely before reporting results.
```
