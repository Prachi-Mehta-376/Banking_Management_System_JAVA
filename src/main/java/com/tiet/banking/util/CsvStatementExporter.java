package com.tiet.banking.util;

import com.tiet.banking.dto.AccountSummary;
import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.exception.ValidationException;
import com.tiet.banking.model.Money;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvStatementExporter {
    private CsvStatementExporter() {
    }

    public static Path export(Path targetFile, AccountSummary summary, List<TransactionView> transactions) {
        if (targetFile == null) {
            throw new ValidationException("Export file path is required.");
        }
        try {
            Path parent = targetFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                writer.write("# Account Number," + sanitizeAndQuote(summary.accountNumber()));
                writer.newLine();
                writer.write("# Account Holder," + sanitizeAndQuote(summary.ownerName()));
                writer.newLine();
                writer.write("# Email," + sanitizeAndQuote(summary.ownerEmail()));
                writer.newLine();
                writer.write("# Current Balance (INR)," + sanitizeAndQuote(Money.formatPlain(summary.balancePaise())));
                writer.newLine();
                writer.write("Reference,Timestamp (UTC),Operation Type,Amount (INR),Balance After (INR),Initiated By");
                writer.newLine();

                for (TransactionView tx : transactions) {
                    writer.write(String.join(",",
                            sanitizeAndQuote(tx.referenceId()),
                            sanitizeAndQuote(tx.timestamp().toString()),
                            sanitizeAndQuote(tx.operationType().name()),
                            sanitizeAndQuote(Money.formatPlain(tx.deltaPaise())),
                            sanitizeAndQuote(Money.formatPlain(tx.balanceAfterPaise())),
                            sanitizeAndQuote(tx.performedByRole())));
                    writer.newLine();
                }
            }
            return targetFile;
        } catch (IOException ex) {
            throw new PersistenceException("Failed to write CSV statement file.", ex);
        }
    }

    // Neutralize leading spreadsheet formula characters before RFC-4180 CSV quoting.
    static String sanitizeAndQuote(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty()) {
            char first = text.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                boolean signedDecimal = (first == '-' || first == '+') && text.matches("^[+-]?\\d+(\\.\\d+)?$");
                if (!signedDecimal) {
                    text = "'" + text;
                }
            }
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
