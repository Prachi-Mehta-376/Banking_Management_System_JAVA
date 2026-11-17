# Team Handoff — Member R: JavaFX Presentation & User Experience (`03_R_JAVAFX_AND_UX.md`)

> **Peer Role:** Equal Module Owner (**R**) — No hierarchical team lead.
> **Scope:** JavaFX Screens, Navigation Shell, Forms, `TableView` Displays, CSS Styling, User Feedback, Mock-Compatible UI Development & Screenshots.
> **Current Status:** `NOT STARTED` (M0 Documentation & Scaffolding Phase)

---

## 1. Module Purpose

The **JavaFX Presentation & User Experience** module provides a clean, responsive, intuitive desktop user interface for all three user roles (**Customer**, **Bank Staff**, **Administrator**). It surfaces service data and error feedback clearly while strictly delegating all business validation, authorization, and persistence to **S**'s service contracts.

---

## 2. Responsibilities

- Design the shared application shell, navigation sidebar/header, and feedback dialogs (`ui/common/`).
- Implement authentication screens (`ui/auth/`): login screen and clean role-based dashboard routing.
- Implement role-specific workspaces:
  - **Customer (`ui/customer/`):** Owned accounts & balance cards, internal transfer form, chronological `TableView` transaction history, P1 statement export file chooser.
  - **Bank Staff (`ui/staff/`):** Synthetic customer registration form, account creation workflow, simulated cash deposit & withdrawal forms, customer lookup & account status view, P1 freeze/unfreeze request form.
  - **Administrator (`ui/admin/`):** P1 pending freeze/unfreeze approval queue, staff account management view, system audit log viewer (`TableView` with filters), balance reconciliation trigger & summary view.
- Author cohesive desktop styling (`src/main/resources/styles/`).
- Co-design UI-facing DTOs (`dto/`) with **S** during M1.
- Capture verified UI screenshots into `docs/report/screenshots/` and contribute UI walkthrough documentation for the UCS553 project report (`M5`).

---

## 3. Owned Directories

| Directory | Ownership Type | Notes |
| :--- | :---: | :--- |
| `src/main/java/com/tiet/banking/ui/common/` | **Primary Owner** | Shared navigation shell, header/logout bar, alert/error banners, formatters |
| `src/main/java/com/tiet/banking/ui/auth/` | **Primary Owner** | Login screen and role routing controller |
| `src/main/java/com/tiet/banking/ui/customer/` | **Primary Owner** | Customer dashboard, transfer form, transaction history & export UI |
| `src/main/java/com/tiet/banking/ui/staff/` | **Primary Owner** | Staff dashboard, customer onboarding, cash deposit/withdrawal & freeze request UI |
| `src/main/java/com/tiet/banking/ui/admin/` | **Primary Owner** | Admin dashboard, approval queue, staff management, audit & reconciliation UI |
| `src/main/resources/styles/` | **Primary Owner** | JavaFX CSS stylesheets |
| `docs/report/screenshots/` | **Primary Owner** | Verified application screenshots for UCS553 report (`M5`) |
| `src/main/java/com/tiet/banking/dto/` | **Co-owned w/ S** | Request/view DTO alignment agreed in M1 |

---

## 4. Dependencies on Other Modules

- **Depends on S (`service/`, `dto/`, `exception/`):** Needs agreed service interfaces, DTO definitions, and custom exception types in M1.
- **Independent Parallel UI Development (Mock / Sample Data Strategy):** **R** does **not** need to wait for **P**'s SQLite database or **S**'s final service logic to begin building JavaFX views. Once M1 service interfaces and DTOs are frozen, **R** can implement temporary in-memory mock service providers (with synthetic sample data) behind those interfaces to test layouts, `TableView` bindings, form validation UX, and navigation immediately, then swap cleanly to **S**'s real services via `bootstrap/` in M3.

---

## 5. Expected Interfaces (Provisional — Pending M1 Freeze)

- **Consumed from S:** Conceptual service interfaces in [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md) (`AuthService`, `CustomerService`, `AccountService`, `TransactionService`, P1 governance/audit services) injected into UI views/controllers via constructor or `bootstrap/` assembly.
- **Consumed Exceptions:** Custom domain/auth/validation exceptions from `exception/` translated into clear, user-friendly JavaFX alerts or inline form validation labels (never raw stack traces).

---

## 6. Integration Boundaries

- **Zero Persistence Imports:** JavaFX code under `ui.*` must **never** import `com.tiet.banking.dao.*`, `com.tiet.banking.db.*`, or `java.sql.*`.
- **Zero Business/Security Shortcuts:** UI input validation (e.g., non-empty fields, numeric format hints) improves usability, but **never** replaces service-layer validation, RBAC enforcement, or balance rules (`ADR-005`).
- **Money Display Hygiene:** Format `BigDecimal` monetary fields cleanly for display; never convert balances into `double` inside UI controllers or `TableView` cell factories (`ADR-003`).
- **Contract Alignment:** Any new data field needed in a JavaFX form or `TableView` must be proposed in `dto/` and reviewed with **S** rather than bypassing service contracts.

---

## 7. Initial Tasks (For M1 -> M3 Transition)

1. Review [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md) and [../architecture/ROLE_PERMISSIONS.md](../architecture/ROLE_PERMISSIONS.md) with **S** to confirm every field required on Customer, Staff, and Admin screens.
2. Co-design shared DTO structures (`dto/`) and screen navigation flow in M1.
3. On branch `feature/javafx-ui` (once M1 contracts freeze), build the common shell, login screen, three role dashboards, and CSS theme—initially runnable against temporary in-memory mock service implementations using synthetic sample data.
4. Coordinate with **S** in M3 (`bootstrap/`) to wire real services and verify end-to-end flows.

---

## 8. Acceptance Criteria

- Clean role-based navigation: Customer, Bank Staff, and Administrator each see only their authorized workspaces and features.
- All P0 forms and `TableView`s (customer registration, account opening, cash deposit/withdrawal, balance enquiry, transfer, transaction history) bind cleanly to DTOs and handle service exceptions gracefully.
- Consistent JavaFX CSS styling, readable typography, clear success/error feedback banners, and zero `java.sql.*` / `dao.*` coupling.
- Verified walkthroughs (`DEMO-01`..`DEMO-03`) documented with real screenshots in `docs/report/screenshots/` during M5.

---

## 9. Testing Responsibilities

- **UI & Mock Walkthrough Verification:** Test form edge cases (empty fields, malformed amounts, error message rendering, table refresh after deposit/transfer) against mock providers during UI construction.
- **End-to-End Manual Checklist (`docs/testing/DEMO_SCENARIOS.md`):** Execute and document manual verification across all three roles once wired to live SQLite services in M3/M4, capturing authentic screenshots in M5.

---

## 10. What Is Explicitly Outside This Module's Ownership

- Implementing password hashing, RBAC security enforcement, or financial transaction business logic (owned by **S**).
- Writing JDBC queries, SQL schemas, DAO implementations, or SQLite connection management (owned by **P**).
- Modifying `service/` implementations directly to work around UI state instead of updating contracts via PR review.

---

## 11. Current Status

- **Status:** `NOT STARTED`
- **Artifacts Existing:** Directory `.gitkeep` placeholders only (`ui/common/`, `ui/auth/`, `ui/customer/`, `ui/staff/`, `ui/admin/`, `resources/styles/`, `docs/report/screenshots/`).
- **Screens & Screenshots:** `0` FXML/JavaFX files, `0` CSS files, `0` screenshots.

---

## 12. Resume Instructions

1. Read [../../AGENTS.md](../../AGENTS.md), [../../context/00_MASTER_PROJECT_CONTEXT.md](../../context/00_MASTER_PROJECT_CONTEXT.md), and [../../context/CURRENT_STATUS.md](../../context/CURRENT_STATUS.md).
2. Review [../architecture/SERVICE_CONTRACTS.md](../architecture/SERVICE_CONTRACTS.md), [../architecture/ROLE_PERMISSIONS.md](../architecture/ROLE_PERMISSIONS.md), and [../requirements/FEATURE_MATRIX.md](../requirements/FEATURE_MATRIX.md).
3. Finalize DTO and service contract fields with **S** in M1 before building JavaFX screens on branch `feature/javafx-ui`.
