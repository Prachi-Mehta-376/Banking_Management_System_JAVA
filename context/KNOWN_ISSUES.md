# Known Issues, Design Risks & Open Questions (`KNOWN_ISSUES.md`)

> **Important:** No application code exists yet. This register tracks **architectural design risks and unresolved decisions**—not fabricated software bugs.

---

## 1. Open Design Risks & Questions Register

### `RISK-01` — Database Transaction Ownership (`OPEN DESIGN QUESTION`)
- **Area:** Service Layer (`S`) <-> Persistence Layer (`P`)
- **Question:** How should multi-DAO atomic operations (e.g., debit source account + credit target account + insert transaction + insert audit log) share a single JDBC `Connection` without leaking `java.sql.Connection` into business service signatures?
- **Options Under Evaluation:**
  1. A `TransactionManager` / `TransactionTemplate` callback in `db/` using a thread-bound or context-bound `Connection` that DAOs retrieve automatically.
  2. Explicit `Connection` or lightweight `UnitOfWork` token passed from service through DAO methods.
- **Impact if Unresolved:** Risk of partial commits (money debited without corresponding credit) or broken layer separation.
- **Related:** `ADR-004` in [DECISIONS.md](DECISIONS.md), [../docs/architecture/TRANSACTION_MODEL.md](../docs/architecture/TRANSACTION_MODEL.md).

### `RISK-02` — Authorization Enforcement Boundary (`DESIGN RISK`)
- **Area:** Presentation (`R`) <-> Service & Security (`S`)
- **Question:** How do we ensure UI navigation guards never become the sole authorization check?
- **Risk:** If role/ownership checks exist only by hiding buttons in JavaFX, a bug or future caller invoking a service method directly could allow cross-customer data exposure or unauthorized balance changes.
- **Mitigation Direction:** Every protected service method must explicitly validate the authenticated session principal, allowed roles, and customer-account ownership (`ADR-005`).

### `RISK-03` — SQLite Local-File Security & Concurrency Limitations (`TECHNICAL LIMITATION`)
- **Area:** Persistence (`P`)
- **Context:** SQLite is an embedded local database file (`data/*.db`) with single-writer locking and no built-in multi-user server authentication.
- **Risks:**
  1. Concurrent writes across threads or background tasks could trigger `SQLITE_BUSY` lock errors unless serialized or configured with busy timeouts and WAL mode.
  2. Any OS user with filesystem read access to the SQLite file can inspect stored tables directly (mitigated educationally via password hashing, never storing plaintext passwords, and documenting simulation limits honestly).

### `RISK-04` — Banking Simulation Money Creation & Conservation Rules (`OPEN DESIGN QUESTION`)
- **Area:** Domain Logic (`S`)
- **Question:** How does simulated money legitimately enter and leave the system?
- **Proposal:** Only **Bank Staff** counter operations (simulated cash deposit / withdrawal) alter total system liability; **Customer transfers** must strictly preserve total system balance (`sum(debits) == sum(credits)`).
- **Open Question:** Should initial customer account creation allow an opening deposit in the same transaction, or always start at `0.00` followed by a Staff Cash Deposit?

### `RISK-05` — Account Lifecycle & State Transition Rules (`OPEN DESIGN QUESTION`)
- **Area:** Domain Model (`S` & `P`)
- **Question:** What exact lifecycle states (`PENDING`, `ACTIVE`, `FROZEN`, `CLOSED`) should an account support, and which operations are permitted in each state?
- **Key Edge Cases to Decide in M1:**
  - Can a `FROZEN` account receive incoming transfers, or must both incoming and outgoing movements be blocked?
  - Can an account with a non-zero balance be marked `CLOSED`?
  - Can Bank Staff freeze directly, or only request a freeze for Admin approval (P1 maker-checker)?

### `RISK-06` — Audit Logging Completeness & Atomicity (`DESIGN RISK`)
- **Area:** Security & Persistence (`S` & `P`)
- **Question:** Should database audit records commit inside the same JDBC transaction as the audited operation, or separately so failed/denied attempts are also logged?
- **Trade-off:** Same-transaction audit ensures committed actions always have an audit row, whereas rolled-back attempts (e.g., repeated unauthorized access or insufficient funds) would also vanish unless logged via an independent mechanism or `java.util.logging`.

### `RISK-07` — Scope Management & Academic Timeline (`PROJECT RISK`)
- **Area:** Project Planning (All Members)
- **Risk:** Over-committing to optional P2 features (disputes, risk scoring, idempotency keys, multiple account sub-types) before P0 core banking flows are tested and UI-integrated could jeopardize UCS553 submission readiness.
- **Mitigation:** Enforce strict milestone gating (`M0` -> `M1` -> `M2` -> `M3` P0 baseline before P1/P2 expansion).

### `RISK-08` — Regulatory Applicability & Overclaiming Risk (`COMPLIANCE BOUNDARY RISK`)
- **Area:** Research & Documentation (All Members)
- **Risk:** Citing unverified RBI circulars or implying the project satisfies real Indian banking regulations would compromise academic integrity.
- **Mitigation:** Keep [../docs/research/REGULATORY_TRACEABILITY.md](../docs/research/REGULATORY_TRACEABILITY.md) explicitly marked pending Gemini Deep Research verification and state clearly that this is an educational simulation.

### `RISK-09` — Cross-Team Integration & Contract Drift (`COLLABORATION RISK`)
- **Area:** Team Coordination (`S`, `P`, `R`)
- **Risk:** Parallel work on separate branches can cause DTO field mismatches between `R` (JavaFX) and `S` (Service), or DAO exception/model mismatches between `P` and `S`.
- **Mitigation:** Freeze shared `model/`, `dto/`, `service/`, and `dao/` interface contracts in M1, allow `R` to build against agreed DTO contracts/mock providers, and require cross-review PRs for any contract edit.
