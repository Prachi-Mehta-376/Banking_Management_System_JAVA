# Known Issues, Design Risks & Open Questions (`KNOWN_ISSUES.md`)

> **Important:** No application code exists yet. This register tracks **architectural design risks and unresolved decisions**—not software defects in implemented code.

---

## 1. Risks Addressed by M1 Proposals (Awaiting Team Approval)

- **`RISK-01` — Database Transaction Ownership (`PROPOSED RESOLUTION IN M1`):** Addressed via `TransactionExecutor`, `TransactionWork<T>`, and `BankingUnitOfWork` (`ADR-004`), ensuring all DAOs in a financial operation share one SQLite `Connection` managed by **P** while business orchestration stays with **S**.
- **`RISK-02` — Authorization Enforcement Boundary (`PROPOSED RESOLUTION IN M1`):** Addressed via service-layer `UserSession` role & single-account `userId` checks (`ADR-005`), non-teller `ADMIN` isolation, and planned test `TC-04`.
- **`RISK-04` — Simulation Liquidity & Opening Balance (`PROPOSED RESOLUTION IN M1`):** Addressed via `ADR-008`: public customer registration initializes `accounts.balance_paise = 0`, only `STAFF` cash deposits/withdrawals add/remove simulated system funds, and internal transfers strictly sum to `0` (`INV-04`).
- **`RISK-05` — Account Lifecycle States (`PROPOSED RESOLUTION IN M1`):** Addressed via v1 `AccountStatus` restriction to `ACTIVE` and `FROZEN` (account closure out of P0 scope), where `FROZEN` permits read-only history/balance viewing and blocks deposits, withdrawals, and both incoming/outgoing transfers (`INV-07`).

---

## 2. Active Design Risks & Open Questions

### `RISK-03` — SQLite Local-File Security & Concurrency Limitations (`TECHNICAL LIMITATION`)
- **Area:** Persistence (`P`)
- **Context:** SQLite is a local desktop database file (`data/*.db`) with single-writer locking and no network server isolation.
- **Residual Risks:**
  1. Concurrent writes can cause `SQLITE_BUSY` unless `db/` configures `busy_timeout`, `BEGIN IMMEDIATE` write locks, and clean connection closing (`TC-12`).
  2. Any OS user with local filesystem access can inspect or modify the `.db` file externally; therefore, local SQLite storage cannot provide cryptographic tamper-proofing (`INV-01`/`INV-09`/`INV-11` apply to application workflows only).

### `RISK-06` — Audit Logging for Rolled-Back / Rejected Operations (`OPEN DESIGN QUESTION`)
- **Area:** Security & Persistence (`S` & `P`)
- **Context:** Successful admin/staff mutations commit `audit_events` in the same JDBC transaction (`INV-10`). However, when a transaction fails or an unauthorized call aborts before commit, SQLite rolls back any writes inside that transaction.
- **Current M1 Approach:** Record rejected/failed attempts via `java.util.logging` (`TECHNICAL_CONTEXT.md`), while evaluating in M1/M4 whether failed login/auth attempts also need an independent `audit_events` write in P1.

### `RISK-07` — Scope Creep vs. 3-Student Academic Deadline (`PROJECT RISK`)
- **Area:** Planning (`S`, `P`, `R`)
- **Risk:** Adding premature abstractions, optional P2 features, or elaborate UI dependencies before P0 (`F-P0-01`–`F-P0-17`) is tested end-to-end threatens on-time UCS553 delivery.
- **Mitigation:** Strictly enforce `AGENTS.md` Section 5 (student-level simplicity) and complete P0 before attempting P1/P2.

### `RISK-08` — Regulatory Applicability & Pending External Research (`COMPLIANCE BOUNDARY RISK`)
- **Area:** Research & Documentation
- **Context:** External Gemini Deep Research output has not yet been reviewed.
- **Mitigation:** Keep [../docs/research/RESEARCH_FINDINGS.md](../docs/research/RESEARCH_FINDINGS.md) and [../docs/research/REGULATORY_TRACEABILITY.md](../docs/research/REGULATORY_TRACEABILITY.md) explicitly marked pending; make zero RBI compliance or production-readiness claims.

### `RISK-09` — Cross-Team Contract Alignment & Open M1 Details (`INTEGRATION RISK`)
- **Area:** Coordination (`S`, `P`, `R`)
- **Open Items Requiring Sign-Off Before M2 Coding:**
  1. Synthetic `account_number` generation format between `S` and `P` ([../docs/architecture/DATABASE_DESIGN.md](../docs/architecture/DATABASE_DESIGN.md)).
  2. Whether P0 financial service signatures (`transferFunds`, `depositCash`, `withdrawCash`) include a nullable `idempotencyKey` placeholder immediately or add it in P1 ([../docs/architecture/SERVICE_CONTRACTS.md](../docs/architecture/SERVICE_CONTRACTS.md)).
  3. Whether `ADMIN` has read-only access to `StaffService.searchCustomers` or strictly uses `AdministrativeSummary` and P1 Audit Viewer ([../docs/architecture/ROLE_PERMISSIONS.md](../docs/architecture/ROLE_PERMISSIONS.md)).

### `RISK-10` — Uncertain Commit Outcome & No Automatic Retries (`RELIABILITY BOUNDARY`)
- **Area:** Transaction Infrastructure (`P` & `S`)
- **Risk:** If an I/O fault happens at `connection.commit()` time, the client cannot tell whether the SQLite commit completed.
- **Mitigation:** P0 strictly forbids automatic retries on financial mutations; persistent `idempotency_key` deduplication is scheduled in P1 (`F-P1-05`, `TC-17`).
