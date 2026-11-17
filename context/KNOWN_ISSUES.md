# Known Issues, Design Risks & Open Questions (`KNOWN_ISSUES.md`)

> **Important:** No application code exists yet. Section 1 summarizes design risks technically resolved by M1 Decisions A–F (awaiting PR #1 approval). Section 2 lists active environmental/project limitations.

---

## 1. Technically Resolved in M1 (`AWAITING PR #1 TEAM APPROVAL`)

- **`RISK-01` — Database Transaction Ownership:** Resolved via `TransactionExecutor`, `TransactionWork<T>`, and `BankingUnitOfWork` (`ADR-004`), sharing one SQLite `Connection` per operation.
- **`RISK-02` — Authorization Enforcement Boundary:** Resolved via service-layer `UserSession` role & single-account `userId` checks (`ADR-005`), non-teller `ADMIN` restriction, and read-only `ADMIN` search (`Decision C`).
- **`RISK-04` — Simulation Liquidity & Opening Balance:** Resolved via `ADR-008`: public customer registration creates 1 `ACTIVE` account at `0` paise; only `STAFF` cash ops add/remove simulated funds; transfers sum to `0` (`INV-04`).
- **`RISK-05` — Account Lifecycle States:** Resolved via v1 `ACTIVE`/`FROZEN` policy (closure out of P0 scope); `FROZEN` permits read-only history/balance viewing and blocks deposits, withdrawals, incoming and outgoing transfers (`INV-07`).
- **`RISK-06` — Audit Atomicity & Failed Attempts (`Decision D`):** Resolved: sensitive state changes write `audit_events` in the same DB transaction so rolled-back transactions never leave misleading success rows; rejected/failed attempts log via `java.util.logging` without passwords or sensitive customer data.
- **`RISK-09` — Cross-Team Contract Details:** All three M1 open items resolved (`Decision A`: `AC000001` from `accounts.id`; `Decision B`: no `idempotencyKey` in P0 APIs; `Decision C`: `ADMIN` gets read-only `StaffService` customer/account search).
- **`RISK-10` — Uncertain Commit Outcomes (`Decision E`):** Resolved: distinguish pre-commit failure, confirmed commit, and `CommitOutcomeUncertainException` on commit I/O failure; never automatically retry financial operations; instruct user to inspect history / `referenceId`.

---

## 2. Active Environmental & Project Risks

### `RISK-03` — SQLite Local-File Security & Concurrency Limitations (`TECHNICAL LIMITATION`)
- **Area:** Persistence (`P`)
- **Context:** SQLite is an embedded desktop database file (`data/*.db`) with single-writer locking and no multi-user network server isolation.
- **Limitations:**
  1. Concurrent writes require `PRAGMA busy_timeout = 5000` and `BEGIN IMMEDIATE` write-lock discipline to avoid `SQLITE_BUSY` failures (`TC-12`).
  2. An OS user with direct filesystem access to `data/*.db` can bypass application controls externally; local SQLite is an academic simulation, not a hardened banking deployment (`Decision F`).

### `RISK-07` — Scope Creep vs. 3-Student Academic Deadline (`PROJECT RISK`)
- **Area:** Planning (`S`, `P`, `R`)
- **Risk:** Adding premature abstractions, duplicate DTOs, or optional P2 features before P0 (`F-P0-01`–`F-P0-17`) is tested threatens UCS553 delivery.
- **Mitigation:** Strictly enforce [../AGENTS.md](../AGENTS.md#5-student-level-engineering-code-quality--practical-collaboration) and complete P0 before P1/P2.

### `RISK-08` — Pending External Gemini Deep Research (`COMPLIANCE BOUNDARY RISK`)
- **Area:** Research & Documentation
- **Context:** External Gemini Deep Research output has not yet been received/reviewed.
- **Mitigation:** Preserve [../docs/research/RESEARCH_FINDINGS.md](../docs/research/RESEARCH_FINDINGS.md) and [../docs/research/REGULATORY_TRACEABILITY.md](../docs/research/REGULATORY_TRACEABILITY.md) as pending placeholders; make zero RBI compliance or regulatory claims.
