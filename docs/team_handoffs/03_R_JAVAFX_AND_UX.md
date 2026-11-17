# Team Handoff — Member R: JavaFX Presentation & User Experience (`03_R_JAVAFX_AND_UX.md`)

> **Peer Role:** Equal Module Owner (**R**) — No hierarchical team lead.
> **Scope:** JavaFX Screens & User Workflows, Visual Design & CSS Styling, Form Controls & Feedback, Account & Transaction Displays, Screenshots & Presentation Documentation.
> **Current Status:** `NOT STARTED` (M1 Architecture & Contract Specification Under Review)

---

## 1. Module Purpose

The **JavaFX Presentation & User Experience** module delivers a clean, cohesive desktop interface across **First-Run Setup / Auth**, **Customer**, **Bank Staff**, and **Administrator** workflows. It renders service-facing DTOs from **S** clearly and translates domain/authorization exceptions into helpful user feedback without embedding business logic or SQL dependencies in the UI.

---

## 2. Responsibilities & M1 / M2 Deliverables

### M1 Deliverables (Current Milestone — Documentation Only)
- Review presentation-facing DTOs (`UserSession`, `AccountSummary`, `TransactionReceipt`, `TransactionView`, `CustomerSearchResult`, `AdministrativeSummary`, plus P1 DTOs) and service contracts in [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md).
- Confirm all UI inputs/outputs needed for Customer, Staff, and Admin workflows and planned demo walkthroughs ([../testing/DEMO_SCENARIOS.md](../testing/DEMO_SCENARIOS.md)).

### M2 / M3 Implementation Tasks (After M1 Sign-Off)
- Build shared shell, role navigation header/sidebar, logout bar, and alert/error banners (`ui/common/`).
- Build First-Run Administrator Setup, Login, and Public Customer Registration views (`ui/auth/`).
- Build Customer workspace (`ui/customer/`): single-account balance & status card, transfer form + receipt display, chronological `TableView` transaction history, P1 CSV statement export file chooser.
- Build Bank Staff workspace (`ui/staff/`): customer/account search table, account inspection, simulated cash deposit & withdrawal forms, P1 freeze/unfreeze request dialog.
- Build Administrator workspace (`ui/admin/`): `AdministrativeSummary` overview, Staff creation/deactivation controls, P1 pending freeze/unfreeze approval table, P1 audit event viewer, P1 balance reconciliation trigger/table.
- Author cohesive, clean JavaFX CSS styling (`src/main/resources/styles/`).
- Capture authentic UI screenshots (`docs/report/screenshots/`) and UI documentation in M5.

---

## 3. Owned Directories

| Directory | Ownership Type | Notes |
| :--- | :---: | :--- |
| `src/main/java/com/tiet/banking/ui/common/` | **Primary Owner** | Shared shell, navigation, alert/error banners, display helpers |
| `src/main/java/com/tiet/banking/ui/auth/` | **Primary Owner** | First-run admin setup, login & public customer registration views |
| `src/main/java/com/tiet/banking/ui/customer/` | **Primary Owner** | Customer balance, transfer form & transaction history `TableView` |
| `src/main/java/com/tiet/banking/ui/staff/` | **Primary Owner** | Customer/account search, cash deposit & withdrawal workflows |
| `src/main/java/com/tiet/banking/ui/admin/` | **Primary Owner** | Admin summary, staff provisioning & P1 governance/audit/reconciliation views |
| `src/main/resources/styles/` | **Primary Owner** | JavaFX CSS stylesheet(s) |
| `docs/report/screenshots/` | **Primary Owner** | Verified application screenshots for final UCS553 report (M5) |
| `src/main/java/com/tiet/banking/dto/` | **Co-owned w/ S** | Presentation DTO alignment agreed in M1 |

---

## 4. Dependencies & Integration Support

- **Depends on S (`service/`, `dto/`, `exception/`):** Consumes agreed M1 DTOs and service interfaces.
- **Independent Mock Development:** Once M1 contracts freeze, **R** can build and test every JavaFX view against lightweight in-memory mock service implementations using fictitious sample data—independently of SQLite progress.
- **Startup & Navigation Collaboration:** **S** can support the application startup and navigation wiring foundation during integration (`bootstrap/` + `ui/common/`) while **R** retains primary ownership of the JavaFX presentation module, layouts, styling, and user workflows.

---

## 5. Expected Interfaces (Provisional — Pending M1 Freeze)

- **Consumed from S:** `AuthService`, `CustomerBankingService`, `TellerService`, `StaffService`, `AdminService`, and P1 `ReconciliationService` / `AccountControlService` ([../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md)).
- **Consumed Exceptions:** Typed domain/validation/auth exceptions translated cleanly into UI feedback messages.

---

## 6. Integration Boundaries

- **Zero Persistence Coupling:** `ui.*` must **never** import `dao.*`, `dao.jdbc.*`, `db.*`, or `java.sql.*`.
- **No Security Shortcuts:** Hiding disallowed tabs improves UX, while service-layer checks remain the true authorization boundary (`ADR-005`).
- **Keep Controllers Simple:** Maintain readable, maintainable JavaFX controllers and clean styling without introducing external UI frameworks, complex animations, or heavy state-management libraries ([../../AGENTS.md](../../AGENTS.md#5-student-level-engineering-code-quality--practical-collaboration)).
- **Money Display Hygiene:** Render formatted strings / `BigDecimal` from DTOs; never cast monetary values into `double` or `float`.

---

## 7. Review Responsibilities

- Review M1 `dto/` definitions and service error semantics with **S** to confirm every JavaFX screen has all required fields without leaking internal persistence models.
- Coordinate with **S** during M3 integration (`bootstrap/`) and verify end-to-end UI flows across all three roles.

---

## 8. Acceptance Criteria

- Startup cleanly routes to First-Run Admin Setup when no admin exists, and Login once initialized.
- Public customer registration, three-role login/logout, staff search, cash deposit/withdrawal, customer transfer, and history `TableView` work smoothly with clear validation/error banners.
- `FROZEN` accounts display clear restriction status and disable/report rejected operations cleanly (P1).
- Zero `dao.*` / `java.sql.*` imports in `ui.*`; styling is consistent and legible.

---

## 9. Testing Responsibilities

- **UI Flow & Form Feedback Checks:** Verify empty/malformed inputs, error banner rendering, and table refreshes against mock providers during M2/M3 and against live SQLite services during M3/M4 (`DEMO-01`–`DEMO-04`).
- **Report Screenshots:** Capture authentic screenshots into `docs/report/screenshots/` once end-to-end flows work in M5.

---

## 10. What Is Explicitly Outside This Module's Ownership

- Implementing `Money` validation rules, password hashing, RBAC enforcement, or JDBC transaction coordination (owned by **S**).
- Authoring SQLite schemas, SQL queries, or JDBC DAOs (owned by **P**).

---

## 11. Current Status

- **Status:** `NOT STARTED`
- **Artifacts Existing:** Directory `.gitkeep` placeholders only; M1 DTO/screen contracts drafted on branch `docs/m1-architecture-contracts`.
- **Screens & Screenshots:** `0` Java/FXML/CSS files, `0` screenshots.

---

## 12. Resume Instructions

1. Read [../../AGENTS.md](../../AGENTS.md), [../../context/CURRENT_STATUS.md](../../context/CURRENT_STATUS.md), and [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md).
2. Confirm DTO field completeness with **S** during M1 review.
3. Once M1 is approved, open branch `feature/javafx-ui` to implement the JavaFX views, CSS styling, and mock-compatible screen walkthroughs.
