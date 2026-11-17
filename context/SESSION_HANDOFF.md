# Session Handoff (`SESSION_HANDOFF.md`)

> **Use This File To:** Resume work cleanly in any new AI chat or IDE session.
> **Important:** Always inspect actual repository contents, `git status`, and branch state before relying solely on this handoff ([../AGENTS.md](../AGENTS.md)).

---

## 1. Current Project State

- **Milestone:** **M1 — Architecture v2.0 & Contract Specification** (Drafted on branch `docs/m1-architecture-contracts`, targeting `main` via PR; awaiting team review).
- **Stable Integration Branch (`main`):** At M0 baseline commit `392994037b4f71201e445c65e7062076844196f7`.
- **Application Code:** **Not Started** (`src/` and `data/` contain `.gitkeep` placeholders only).
- **Build Configuration:** **Not Created** (`pom.xml` intentionally deferred until M2).
- **Tests:** **Not Implemented / Not Executed** (`17` planned scenarios `TC-01`–`TC-17` defined in [../docs/testing/TEST_PLAN.md](../docs/testing/TEST_PLAN.md); `0` executed).

---

## 2. Most Recent Work Summary

- **M0 (Committed on `main` at `3929940`):** Initialized directory tree, `.gitignore`, `AGENTS.md`, `README.md`, `context/`, `docs/`, and 25 `.gitkeep` placeholders.
- **M1 (On `docs/m1-architecture-contracts`):**
  - Added Student-Level Engineering, Strict Comment Policy & Anti-Overengineering standards to [../AGENTS.md](../AGENTS.md).
  - Specified M1 v2.0 architecture: integer-paise monetary model (`ADR-003`), balance-plus-transaction-journal (`ADR-007`), 5 core tables (`users`, `accounts`, `operations`, `transactions`, `audit_events` + optional P1 `account_action_requests`), `TransactionExecutor`/`BankingUnitOfWork` contract (`ADR-004`), `INV-01`–`INV-12`, service/DTO contracts, role/bootstrap/frozen-account policies (`ADR-008`), `TC-01`–`TC-17`, and updated `S`/`P`/`R` M1/M2 handoffs.

---

## 3. Essential Documents for the Next Session

1. [../AGENTS.md](../AGENTS.md) — Mandatory onboarding, engineering rules, student code simplicity & comment policy, Git workflow.
2. [00_MASTER_PROJECT_CONTEXT.md](00_MASTER_PROJECT_CONTEXT.md) & [CURRENT_STATUS.md](CURRENT_STATUS.md) — Identity and current status.
3. [DECISIONS.md](DECISIONS.md) & [KNOWN_ISSUES.md](KNOWN_ISSUES.md) — `ADR-001`–`ADR-008` awaiting team approval + `RISK-01`–`RISK-10`.
4. Authoritative M1 Specs:
   - [../docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md) (`INV-01`–`INV-12` & simplifications)
   - [../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md) (5-table schema & 9 correctness questions)
   - [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md) (`TransactionExecutor` & SQLite lock semantics)
   - [../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md) (Models, DTOs, Services, DAOs)
   - [../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md) (Role matrix, first-run bootstrap, `FROZEN` behavior)
5. Teammate Handoffs: [01_S_BANKING_ENGINE.md](../docs/team_handoffs/01_S_BANKING_ENGINE.md), [02_P_DATABASE_AND_JDBC.md](../docs/team_handoffs/02_P_DATABASE_AND_JDBC.md), [03_R_JAVAFX_AND_UX.md](../docs/team_handoffs/03_R_JAVAFX_AND_UX.md).

---

## 4. Pending Review Gates & Open Decisions Before M2

1. **M1 PR Review & Merge (`docs/m1-architecture-contracts` -> `main`):** Team (`S`, `P`, `R`) reviews contracts, resolves the 3 open contract items in `RISK-09` (account number format, optional `idempotencyKey` parameter on P0 methods, and whether Admin has read-only customer search), and transitions approved ADRs to `ACCEPTED`.
2. **External Gemini Deep Research:** Still `PENDING REVIEW` ([../docs/research/RESEARCH_FINDINGS.md](../docs/research/RESEARCH_FINDINGS.md), [REGULATORY_TRACEABILITY.md](../docs/research/REGULATORY_TRACEABILITY.md)). Keep zero unverified compliance claims.

---

## 5. Exact Resume Instructions for Future AI Sessions

```text
1. Run `git status -sb`, `git branch -a`, `git log -1 --oneline`, and inspect `src/`.
2. Read `AGENTS.md`, `context/CURRENT_STATUS.md`, and `context/DECISIONS.md`.
3. Confirm whether M1 PR has been approved/merged to `main` and whether `ADR-001`–`ADR-008` are ACCEPTED.
4. For M2 work, branch from updated `main` (`feature/project-foundation`, `feature/database-jdbc`, `feature/banking-engine`, `feature/javafx-ui`), follow AGENTS.md Section 5 simplicity/comment rules, and never claim unexecuted builds or tests.
5. Update `CURRENT_STATUS.md`, `IMPLEMENTATION_STATUS.md`, `SESSION_LOG.md`, and `SESSION_HANDOFF.md` before ending any implementation session.
```
