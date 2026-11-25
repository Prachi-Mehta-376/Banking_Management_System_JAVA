# Known Issues, Limitations & Operational Notes (`KNOWN_ISSUES.md`)

> **Last Updated:** 2026-11-25 (Post-Implementation Verification)

---

## 1. Verified Design Safeguards

The M1 design risks (`RISK-01`, `RISK-02`, `RISK-04`, `RISK-05`, `RISK-06`, `RISK-09`, `RISK-10`) are implemented in Java and verified via `mvn clean test` (`12/12` passing):
- Shared-connection atomicity and rollback via `JdbcTransactionExecutor` + `BEGIN IMMEDIATE`.
- Service-layer RBAC, non-teller `ADMIN`, and single-account `userId` ownership checks.
- Atomic `AC000001` assignment in `JdbcAccountDao.createAccount`.
- Uncertain commit protection via `CommitOutcomeUncertainException` without auto-retries, plus P1 persistent `idempotency_records`.
- Non-destructive reconciliation detecting discrepancies without overwriting `accounts.balance_paise`.

---

## 2. Remaining Limitations & Open Context Items

### `RISK-03` — Local SQLite File Threat Boundary (`INHERENT SIMULATION LIMITATION`)
- `data/banking.db` is a local SQLite file on the host OS. Any user with direct filesystem access to the database file can inspect or edit rows using external tools. Reconciliation detects stored-vs-journal mismatches after the fact rather than providing cryptographic tamper-proofing.

### `RISK-08` — External Gemini Deep Research (`PENDING REVIEW`)
- [../docs/research/RESEARCH_FINDINGS.md](../docs/research/RESEARCH_FINDINGS.md) and [../docs/research/REGULATORY_TRACEABILITY.md](../docs/research/REGULATORY_TRACEABILITY.md) remain pending external research placeholders. Zero RBI compliance or statutory claims are made.

### `RISK-11` — Deferred Optional Features (`DEFERRED P2`)
- Fraud alerts (`F-P2-01`), transaction disputes (`F-P2-02`), and multi-account types (`F-P2-03`) were intentionally deferred so the P0 + P1 baseline remains stable and 100% tested for the 26 Nov 2026 submission.
