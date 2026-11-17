# Regulatory & Domain Traceability (`REGULATORY_TRACEABILITY.md`)

> **Status:** `PLACEHOLDER — PENDING GEMINI DEEP RESEARCH OUTPUT`
> **Important Academic Disclaimer:** This application is an **educational simulation** using synthetic data and simulated funds. It makes **no claim** of Reserve Bank of India (RBI) compliance, legal certification, statutory audit readiness, or production financial security. No legal citations or effective dates have been populated yet; all rows below await verified external research review.

---

## 1. Purpose

When reviewing external research on Indian banking practices, this table will record **conceptual inspiration** and **explicit simulation limitations**—making clear to UCS553 evaluators both what enterprise banking concepts inspired a control and where our Core Java desktop simulation intentionally simplifies real-world regulations.

---

## 2. Traceability Matrix Template (Unpopulated Until Research Review)

| Trace ID | Indian Banking Law / RBI Direction (To Be Verified) | Effective Date | Conceptual Applicability to Simulation | Proposed Educational Control in App | Verified Evidence Source | Implementation & Compliance Limitations |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| `REG-01` | *Pending research verification* (e.g., Customer Identification / Synthetic KYC concept) | `TBD (Pending Source)` | Synthetic customer record attribution during Staff onboarding | Capture synthetic profile fields and staff creator ID (`F-P0-02`) | *Pending Gemini Deep Research* | Uses fictional data only; zero Aadhaar/PAN/e-KYC API verification |
| `REG-02` | *Pending research verification* (e.g., Internal Controls / Maker-Checker Governance) | `TBD (Pending Source)` | Separation of duties for sensitive account restrictions | Staff initiates freeze/unfreeze (`F-P1-01`); Admin approves (`F-P1-02`) | *Pending Gemini Deep Research* | Local single-machine desktop simulation; no organizational IAM/PKI |
| `REG-03` | *Pending research verification* (e.g., Audit Trail & Record Preservation concepts) | `TBD (Pending Source)` | Traceable ledger & administrative audit log | Immutable transaction history & `audit_logs` table (`F-P1-03`) | *Pending Gemini Deep Research* | Local SQLite file lacks hardware WORM storage or external SIEM forwarding |
| `REG-04` | *Pending research verification* (e.g., Transaction Monitoring / Customer Dispute concepts) | `TBD (Pending Source)` | Optional explainable alerts & dispute records (`P2`) | Deterministic threshold alerts (`F-P2-02`) & dispute queue (`F-P2-03`) | *Pending Gemini Deep Research* | Optional P2 scope only; no real-time AML/regulatory reporting feeds |
| `REG-05` | *Pending research verification* (e.g., Access Control & Authentication Hygiene) | `TBD (Pending Source)` | Credential protection & role isolation | Salted password hashing + service-layer RBAC (`ADR-005`) | *Pending Gemini Deep Research* | No MFA, OTP SMS gateway, HSM key custody, or TLS network transport |

---

## 3. Rules for Updating This Document

1. Never insert an Act name, Section number, RBI Master Direction title, or circular effective date without first verifying the exact reference from reviewed research documentation.
2. Always pair any conceptual control with its concrete simulation limitation in the rightmost column.
3. Update [../../context/DECISIONS.md](../../context/DECISIONS.md) and [../requirements/FEATURE_MATRIX.md](../requirements/FEATURE_MATRIX.md) if research alters P1/P2 scope.
