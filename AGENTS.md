# AI Agent & Contributor Engineering Rules (`AGENTS.md`)

This file governs all AI coding assistants and engineering sessions working in the **Banking Management System (UCS553 — Enterprise Web Application)** repository.

> **Current Phase Notice:** This repository is in its **M0 Documentation & Architectural Scaffolding** phase. Verify `context/CURRENT_STATUS.md` and actual repository contents before assuming any code, schema, build file, or test exists.

---

## 1. Mandatory Session Onboarding Sequence

Before proposing changes, writing documentation, or authoring code in any session, follow these steps in order:

1. **Inspect actual repository and Git state first:** Run `git status`, `git branch`, and inspect the workspace directory tree rather than trusting potentially stale context documents.
2. **Read the Master Context:** [context/00_MASTER_PROJECT_CONTEXT.md](context/00_MASTER_PROJECT_CONTEXT.md).
3. **Review Current Status and Session Handoff:** [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md) and [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md).
4. **Review Pending Decisions and Open Risks:** [context/DECISIONS.md](context/DECISIONS.md) and [context/KNOWN_ISSUES.md](context/KNOWN_ISSUES.md).
5. **Read the Relevant Teammate Handoff:**
   - **S (Banking Engine, Security & Integration):** [docs/team_handoffs/01_S_BANKING_ENGINE.md](docs/team_handoffs/01_S_BANKING_ENGINE.md)
   - **P (Database, JDBC & Reliability):** [docs/team_handoffs/02_P_DATABASE_AND_JDBC.md](docs/team_handoffs/02_P_DATABASE_AND_JDBC.md)
   - **R (JavaFX Presentation & UX):** [docs/team_handoffs/03_R_JAVAFX_AND_UX.md](docs/team_handoffs/03_R_JAVAFX_AND_UX.md)

---

## 2. Mandatory Development & Architectural Rules

All future sessions must obey these 18 rules:

1. **Read master context before substantial work:** Never start modifying architecture or writing features without reading [context/00_MASTER_PROJECT_CONTEXT.md](context/00_MASTER_PROJECT_CONTEXT.md).
2. **Review current status and session handoff:** Always align with [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md) and [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md).
3. **Read the relevant teammate handoff:** Check the owner's handoff document under `docs/team_handoffs/` before touching any module.
4. **Respect established architecture decisions:** Follow accepted decisions in [context/DECISIONS.md](context/DECISIONS.md); do not silently bypass or contradict them.
5. **Preserve presentation/service/DAO boundaries:** Maintain strict one-way layering (`ui` -> `service` -> `dao` -> `db`). JavaFX classes (`com.tiet.banking.ui.*`) must never import `dao.*`, `db.*`, or `java.sql.*`.
6. **Keep financial operations inside authorized transaction paths:** Never implement deposits, withdrawals, transfers, or balance mutations outside the business service layer and approved database transaction boundaries.
7. **Preserve authorization and customer-account ownership restrictions:** Role checks (`Customer`, `Bank Staff`, `Administrator`) and customer ownership rules must be enforced at the service boundary—never rely solely on UI visibility.
8. **Never use floating-point storage or arithmetic for money:** Never use `float` or `double` for monetary balances or transaction amounts. Follow the approved fixed-precision strategy (`BigDecimal` in Java and exact integer minor units / fixed decimal storage in SQLite per `ADR-003` once accepted).
9. **Use parameterized SQL statements:** When JDBC implementation begins, always use `PreparedStatement` with bound parameters. Never concatenate user input into SQL strings.
10. **Use only synthetic customer data:** All sample customers, accounts, credentials, and funds must be purely fictional and simulated. Never insert real PII or real financial records.
11. **Avoid unsupported regulatory or security claims:** Do not claim full RBI compliance, legal certification, PCI-DSS compliance, or production-grade banking security. Document simulation boundaries honestly in [docs/research/REGULATORY_TRACEABILITY.md](docs/research/REGULATORY_TRACEABILITY.md).
12. **Never fabricate tests or implementation progress:** Only report code, schemas, builds, or tests as implemented/passing if they exist in the repository and were actually executed and observed passing in the current session.
13. **Respect teammates' module ownership:** Coordinate across `S`, `P`, and `R` boundaries before editing files outside the assigned module scope.
14. **Treat proposed contracts as provisional until approved:** Service contracts, database schemas, and ADRs marked `PROPOSED` or `PROVISIONAL` must be reviewed and approved before dependent code relies on them.
15. **Record architecture changes in the decisions log:** Any new architectural choice or change to an existing decision must be added as an ADR entry in [context/DECISIONS.md](context/DECISIONS.md).
16. **Update documentation after meaningful work:** Keep [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md), [context/IMPLEMENTATION_STATUS.md](context/IMPLEMENTATION_STATUS.md), [context/SESSION_LOG.md](context/SESSION_LOG.md), and [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md) truthful and current after meaningful sessions.
17. **Stay within the UCS553 technology syllabus:** Stick to Java 17, JavaFX, SQLite, Plain JDBC, Maven, DAO pattern, Collections/Generics, Custom Exceptions, Java I/O, and `java.util.logging`. Do not introduce Spring Boot, Hibernate/JPA, React, microservices, or external frameworks.
18. **Never modify unrelated files without justification:** Keep changes scoped to the active task; preserve existing documentation and teammate work.

---

## 3. Documentation Authority Hierarchy

When resolving ambiguity or contradictions across files, follow this strict hierarchy:

| Priority | Source | Authority Scope |
| :---: | :--- | :--- |
| 1 | **Actual Source Code (`src/main/`)** | Ground truth for implemented behavior (once implementation begins) |
| 2 | **Actual Tests (`src/test/`)** | Ground truth for verified behavior (once tests exist and are executed) |
| 3 | [context/00_MASTER_PROJECT_CONTEXT.md](context/00_MASTER_PROJECT_CONTEXT.md) | Stable project identity, course scope, and confirmed boundaries |
| 4 | [docs/requirements/REQUIREMENTS.md](docs/requirements/REQUIREMENTS.md) & [FEATURE_MATRIX.md](docs/requirements/FEATURE_MATRIX.md) | Intended functionality and priority tiers |
| 5 | [context/DECISIONS.md](context/DECISIONS.md) (Accepted entries) | Binding architectural rationale |
| 6 | `docs/architecture/*` | Approved (or provisional) component & interface contracts |
| 7 | [context/CURRENT_STATUS.md](context/CURRENT_STATUS.md) & [IMPLEMENTATION_STATUS.md](context/IMPLEMENTATION_STATUS.md) | Latest summarized project progress |
| 8 | [context/SESSION_HANDOFF.md](context/SESSION_HANDOFF.md) | Session-to-session continuation instructions |

**Conflict Resolution Rule:**
- Documentation must never falsely claim that unimplemented code or tests exist.
- If two documents disagree, determine which source holds higher authority in the table above (or check actual Git/code state), note the discrepancy, and update the stale document rather than duplicating conflicting summaries.
- Prefer relative Markdown links over duplicating long sections across documents.

---

## 4. Git & Collaboration Conventions

- **Shared Repository:** All three teammates (`S`, `P`, `R`) collaborate in one shared GitHub repository.
- **Stable Integration Branch:** `main` is the stable integration branch. Do not push direct unreviewed changes to `main`.
- **Short-Lived Feature Branches:** Work on focused branches and integrate via Pull Requests with cross-review by teammates.
  - Suggested branch naming conventions:
    - `feature/project-foundation`
    - `feature/banking-engine`
    - `feature/database-jdbc`
    - `feature/javafx-ui`
    - `feature/audit-controls`
    - `docs/project-report`
- **Contract Changes:** Any PR that modifies shared DTOs, service boundaries, DAO contracts, or database schema must document the change in the corresponding `docs/architecture/` file and `context/DECISIONS.md`.
- **Verification Before Merge:** Every PR into `main` must include reproducible verification notes (compilation, tests run, manual checks performed) matching the actual state of the branch.
- **Workspace Hygiene:** Never create nested Git repositories inside the workspace, never alter remote ownership/permissions without authorization, and never commit runtime `.db` files, logs, or IDE config clutter.
