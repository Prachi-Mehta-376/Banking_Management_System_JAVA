package com.tiet.banking.dto;

import java.time.Instant;
import java.util.List;

public record ReconciliationReport(
        int totalAccountsChecked,
        int consistentAccounts,
        int mismatchedAccounts,
        int integrityRuleViolations,
        List<ReconciliationResult> accountResults,
        List<String> journalFindings,
        Instant checkedAt) {
}
