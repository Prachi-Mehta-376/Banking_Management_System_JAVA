package com.tiet.banking.dao;

import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.model.BankTransaction;
import java.util.List;

public interface TransactionDao {
    BankTransaction insert(BankTransaction transaction);

    List<TransactionView> findViewsByAccountId(long accountId, int limit);

    List<BankTransaction> findByAccountIdAsc(long accountId);

    List<BankTransaction> findByOperationId(long operationId);

    long sumDeltasByAccountId(long accountId);
}
