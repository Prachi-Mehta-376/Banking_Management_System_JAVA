package com.tiet.banking.exception;

public class CommitOutcomeUncertainException extends PersistenceException {
    private final String referenceId;

    public CommitOutcomeUncertainException(String message, String referenceId, Throwable cause) {
        super(message, cause);
        this.referenceId = referenceId;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
